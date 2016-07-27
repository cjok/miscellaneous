#include <hardware/hardware.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>
#include <cutils/log.h>
#include <cutils/atomic.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>
#include <dirent.h>
#include <stdio.h>      
#include <stdlib.h>    
#include <sys/types.h>   
#include <unistd.h>

#include <hardware/serial.h>

static int ctrl_fd;	//control module fd

static int serial_device_close(struct hw_device_t* device)
{
	ALOGD("%s E", __func__);
	serial_control_device_t* ctx = (serial_control_device_t*)device;
	if (ctx) {
		free(ctx);
	}
	close(ctrl_fd);
	ALOGD("%s X", __func__);
	return 0; 
}

static int serial_read(int fd, serial_control_device_t *dev, char *buf, int count)
{	
	ALOGD("%s E", __func__);
	int len = 0;
	len = read(fd, buf, count);
	if(len < 0)
	{
		perror("read");
	}
	ALOGD("%s X", __func__);
	return len;
}

static int serial_write(int fd, serial_control_device_t *dev, char *buf, int size)
{	
	ALOGD("%s E", __func__);
	int len = write(fd, buf, size);
	if(len < 0)
	{
		perror("write");
	}
	ALOGD("%s X", __func__);
	return len;
}

static int ctrl_select_command(struct ctrl_serial_packet *sp, int type, 
	int data1, int data2)
{
	switch (type) {
		case TYPE_CONTROL_GET_SPEED_CONFIG:
			sp->command = 0x2;
			sp->length = 5;
			break;

		case TYPE_CONTROL_SET_SPEED_CONFIG:
			sp->command = 0x1;
			sp->length = 7;
			sp->data_buf[0] = (char)data1;
			sp->data_buf[1] = (char)data2;
			break;

		case TYPE_CONTROL_GET_CONTROL_STATUS:
			sp->command = 0x13;
			sp->length = 5;
			break;

		case TYPE_CONTROL_GET_TRAFFIC_STATUS:
			sp->command = 0x3;
			sp->length = 5;
			break;

		case TYPE_CONTROL_SYNC_STATUS:
			sp->command = 0x12;
			sp->length = 7;
			sp->data_buf[0] = (char)data1;
			sp->data_buf[1] = (char)data2;
			break;

		default:
			ALOGE("%s: unknow type = %d\n", __func__, type);
			return -1;

	}

	return 0;
}

static int ctrl_create_cmd_line(struct ctrl_serial_packet *sp, int type,
	int data1, int data2)
{
	unsigned int sum;
	int i, ret;

	memset(cmd_line, '\0', sizeof(cmd_line));

	ret = ctrl_select_command(sp, type, data1, data2);
	if (ret == -1) {
		return -1;
	}

	sp->start = 0xaa;
	sp->end = 0x55;

	sum = sp->start + sp->command + sp->length;

	if (sp->length == 7) {
		for (i = 0; i < 2; i++) 
			sum += sp->data_buf[i];
	
		sp->check_sum = (char) (sum & 0xff);

//		sprintf(cmd_line, "%x %x %x %x %x %x %x", sp->start, sp->command, sp->length, 
//			sp->data_buf[0], sp->data_buf[1], sp->check_sum, sp->end);
		cmd_line[0] = sp->start;
		cmd_line[1] = sp->command;
		cmd_line[2] = sp->length;
		cmd_line[3] = sp->data_buf[0];
		cmd_line[4] = sp->data_buf[1];
		cmd_line[5] = sp->check_sum;
		cmd_line[6] = sp->end;

	} else {
		sp->check_sum = (char) (sum & 0xff);

		cmd_line[0] = sp->start;
		cmd_line[1] = sp->command;
		cmd_line[2] = sp->length;
		cmd_line[3] = sp->check_sum;
		cmd_line[4] = sp->end;

//		sprintf(cmd_line, "%x %x %x %x %x", sp->start, sp->command, sp->length, 
//			sp->check_sum, sp->end);
	}

	ALOGE("%s: cmd_line: ", __func__);
	for (i = 0; i < sp->length; i++) {
		ALOGE("%x ", cmd_line[i]);
	}
	
	return 0;
}

//polling /dev/ttyXXX after sended command
static int serial_select(int fd, unsigned char *buf)
{
	int ret, size;
	fd_set readfd;
	struct timeval timeout;

	while (1) {
		timeout.tv_sec = 3;
		timeout.tv_usec = 0;
		FD_ZERO(&readfd);
		FD_SET(fd, &readfd);
		
		ret = select(fd + 1, &readfd, NULL, NULL, &timeout);
		if (ret == -1) {
			ALOGE("%s: select error\n", __func__);
			return -1;
		} else if (ret == 0) {
			ALOGE("%s: select timeout\n", __func__);
			return -1;
		} else {
			if (FD_ISSET(fd, &readfd)) {
				size = read(fd, buf, MAX_RECV_BUF_SIZE);
				if (size <= 0) {
					ALOGE("%s: read failed: %s\n", __func__, strerror(errno));
				}
				
				ALOGE("%s: read size: %d\n", __func__, size);

				return size;
			}
		}
	}
}

static void process_recv_packet(unsigned char *buf, unsigned char *result_buf) 
{
	char *delim = " ";
	char *p;
	int i = 0;
	int n;
	
	p = strtok(buf, delim);
	result_buf[0] = (unsigned char )strtol(p, NULL, 16);
	ALOGE("%s , %x ", p, i);
	while ((p = strtok(NULL, delim))) {
		i++;
		result_buf[i] = (unsigned char )strtol(p, NULL, 16);
		ALOGE("%s , %x ", p, i);
	}

	for (n = 0; n < i; n++) 
		ALOGE("0x%x ", result_buf[i]);
	ALOGE("\n");
}

static int checksum_result(unsigned char *result_buf)
{
	int result_length = result_buf[2];	//command length
	int i = 0; 
	int sum = 0;
	unsigned char checksum = 0;

	for (i = 0; i < result_length - 2; i++) {
		sum += result_buf[i];
	}

	checksum = (unsigned char) sum & 0xff;
	ALOGE("%s: checksum = %x!\n", __func__, checksum);

	if ((result_buf[0] == 0xaa) && (result_buf[result_length - 2] == checksum)) {
		ALOGE("%s: check sum ok!\n", __func__);
		return 0;
	}

	return -1;
}

static int serial_ctrl_set_speed_config(unsigned char *result_buf)
{
//	return 66;
	
	int result;
	
	result = result_buf[3];

	if (result == 0) {
		ALOGE("%s: send success!!\n", __func__);
		return 0;
	} else {
		ALOGE("%s: send failed!!\n", __func__);
		return -1;
	}
}

static int serial_ctrl_sync_status(unsigned char *result_buf)
{
	int result;
	
	result = result_buf[3];

	if (result == 0) {
		ALOGE("%s: send success!!\n", __func__);
		return 0;
	} else {
		ALOGE("%s: send failed!!\n", __func__);
		return -1;
	}
}

static int serial_ctrl_get_speed_config(unsigned char *result_buf, unsigned int *buf)
{
	buf[0] = result_buf[3];	//speed limit
	buf[1] = result_buf[4];	//speed mode
//	buf[0] = 0x33;	//speed limit
//	buf[1] = 0x66;	//speed mode

	ALOGE("%s: speed_limit = %d, speed_mode = 0x%x\n", 
		__func__, buf[0], buf[1]);

	return 0;
}

static int serial_ctrl_get_status(unsigned char *result_buf, int *buf)
{

	buf[0] = result_buf[3] | (result_buf[4] << 8);	//status
	buf[1] = result_buf[5] | (result_buf[6] << 8);
	buf[2] = result_buf[7] | (result_buf[8] << 8);
	buf[3] = result_buf[9] | (result_buf[10] << 8);
	buf[4] = result_buf[11] | (result_buf[12] << 8);
	buf[5] = result_buf[13] | (result_buf[15] << 8);

	return 0;
}

static int serial_ctrl_get_traffic_status(unsigned char *result_buf, int *buf)
{
	buf[0] = result_buf[3];	//controller error
	buf[1] = result_buf[4];	//battery error
	buf[2] = result_buf[5];	//motor error
	buf[3] = result_buf[6];	//communication error
	buf[4] = result_buf[7];	//others
	buf[5] = result_buf[8];	//others

	return 0;
}

static int serial_get_ctrl_info_from_type(unsigned char *result_buf, int type,
	int *buf)
{
	int ret = -1;

	switch (type) {
		case TYPE_CONTROL_GET_SPEED_CONFIG: 
			ret = serial_ctrl_get_speed_config(result_buf, buf);
			break;

		case TYPE_CONTROL_SET_SPEED_CONFIG: 
			ret = serial_ctrl_set_speed_config(result_buf);
			break;

		case TYPE_CONTROL_SYNC_STATUS: 
			ret = serial_ctrl_sync_status(result_buf);
			break;

		case TYPE_CONTROL_GET_CONTROL_STATUS: 
			ret = serial_ctrl_get_status(result_buf, buf);
			break;
	
		case TYPE_CONTROL_GET_TRAFFIC_STATUS: 
			ret = serial_ctrl_get_traffic_status(result_buf, buf);
			break;

		default:
			ALOGE("%s: no type = %d\n", __func__, type);
			break;
	}

	return ret;
}

static int serial_control_info(serial_control_device_t *dev, int type,
	int data1, int data2, int *buf)
{
#if 1
	struct ctrl_serial_packet sp;
	int ret;
	unsigned char recv_buf[MAX_RECV_BUF_SIZE];
	int i;
//	unsigned char result_buf[MAX_RECV_BUF_SIZE];

	ret = ctrl_create_cmd_line(&sp, type, data1, data2);
	if (ret == -1) {
		ALOGE("ctrl_create_cmd_line failed\n");
		return -1;
	}
	
	ret = serial_write(ctrl_fd, dev, cmd_line, sp.length);
	if (ret <= 0) {
		ALOGE("serial_write failed, ret = %d\n", ret);
		return -1;
	}

#if 1
	ret = serial_select(ctrl_fd, recv_buf);
	if (ret <= 0) {
		return -1;
	}

	ALOGE("recv_buf: ");
	for (i = 0; i < ret; i++) {
		ALOGE("%x ", recv_buf[i]);
	}
	printf("\n");

	ret = checksum_result(recv_buf);
	if (ret == -1) {
		return -1;
	}
#endif

	//process_recv_packet(recv_buf, result_buf);

	ret = serial_get_ctrl_info_from_type(recv_buf, type, buf);

	return ret;
	#endif
}

/******************************************************************* 
* 功能：                设置串口数据位，停止位和效验位 
* 入口参数：        fd        串口文件描述符 
*                              speed     串口速度 
*                              flow_ctrl   数据流控制 
*                           databits   数据位   取值为 7 或者8 
*                           stopbits   停止位   取值为 1 或者2 
*                           parity     效验类型 取值为N,E,O,,S 
*出口参数：          正确返回为1，错误返回为0 
*******************************************************************/    
int serial_set(int fd,int speed,int flow_ctrl,int databits,int stopbits,int parity)  
{  
     
    int   i;  
	int   status;  
    int   speed_arr[] = {B921600, B115200, B19200, B9600, B4800, B2400, B1200, B300};  
    int   name_arr[] = {921600, 115200,  19200,  9600,  4800,  2400,  1200,  300};  
           
    struct termios options;  
	
     /*tcgetattr(fd,&options)得到与fd指向对象的相关参数，并将它们保存于options,
	 *该函数还可以测试配置是否正确，该串口是否可用等。若调用成功，函数返回值为0，
	 *若调用失败，函数返回值为1. 
    */ 
    if  ( tcgetattr( fd,&options)  !=  0) {  
          perror("SetupSerial 1");      
          return -1;   
    }  
    //设置串口输入波特率和输出波特率  
    for ( i= 0;  i < sizeof(speed_arr) / sizeof(int);  i++) {  
        if  (speed == name_arr[i]) {               
            cfsetispeed(&options, speed_arr[i]);   
            cfsetospeed(&options, speed_arr[i]);    
        }  
	}       
    //修改控制模式，保证程序不会占用串口 
    options.c_cflag |= CLOCAL;  
	//修改控制模式，使得能够从串口中读取输入数据  
    options.c_cflag |= CREAD;  
	
    //设置数据流控制
    switch(flow_ctrl) 
    {  
        
       case 0 ://不使用流控制 
              options.c_cflag &= ~CRTSCTS;  
              break;     
        
       case 1 ://使用硬件流控制  
              options.c_cflag |= CRTSCTS;  
              break;  
       case 2 ://使用软件流控制
              options.c_cflag |= IXON | IXOFF | IXANY;  
              break;  
    }  
	//设置数据位  
    //屏蔽其他标志位 
    options.c_cflag &= ~CSIZE;  
    switch (databits)  
    {    
       case 5    :  
                     options.c_cflag |= CS5;  
                     break;  
       case 6    :  
                     options.c_cflag |= CS6;  
                     break;  
       case 7    :      
                 options.c_cflag |= CS7;  
                 break;  
       case 8:      
                 options.c_cflag |= CS8;  
                 break;    
       default:     
                 fprintf(stderr,"Unsupported data size\n");  
                 return -1;   
    }  
	
	//设置校验位
    switch (parity)  
    {    
       case 'n':  //无奇偶校验位
       case 'N': 
                 options.c_cflag &= ~PARENB;   
                 options.c_iflag &= ~INPCK;      
                 break;   
       case 'o':    //设置为奇校验
       case 'O':
                 options.c_cflag |= (PARODD | PARENB);   
                 options.c_iflag |= INPCK;               
                 break;   
       case 'e':   //设置为偶校验
       case 'E':
                 options.c_cflag |= PARENB;         
                 options.c_cflag &= ~PARODD;         
                 options.c_iflag |= INPCK;        
                 break;  
       case 's':  //设置为空格  
       case 'S': 
                 options.c_cflag &= ~PARENB;  
                 options.c_cflag &= ~CSTOPB;  
                 break;   
        default:    
                 fprintf(stderr,"Unsupported parity\n");      
                 return -1;   
    }   
	// 设置停止位 
    switch (stopbits) {    
       case 1:     
            options.c_cflag &= ~CSTOPB; break;   
       case 2:     
            options.c_cflag |= CSTOPB; break;  
       default:     
            fprintf(stderr,"Unsupported stop bits\n");   
			return -1;  
    }  
     //修改输出模式，原始数据输出  
	options.c_oflag &= ~OPOST;  
	
	options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
	//options.c_lflag &= ~(ISIG | ICANON);  
     
    //设置等待时间和最小接收字符  
    options.c_cc[VTIME] = 1; /* 读取一个字符等待1*(1/10)s */    
    options.c_cc[VMIN] = 1; /* 读取字符的最少个数为1 */  
     
    //如果发生数据溢出，接收数据，但是不再读取 刷新收到的数据但是不读  
    tcflush(fd,TCIFLUSH);  
     
    //激活配置 (将修改后的termios数据设置到串口中）  
    if (tcsetattr(fd,TCSANOW,&options) != 0) {  
		perror("com set error!\n");    
        return -1;   
    }  
	
    return 0;   
}  

static int base_serial_device_open(const char *dev_path)
{
	int fd;
	struct termios opt; 
	int ret = -1;

	fd = open(dev_path, O_RDWR | O_NOCTTY | O_NDELAY);
	if(fd < 0) {
		ALOGE("%s: %s open: %s\n", __func__, dev_path, strerror(errno));
		return -1;
	}

	ALOGE("fd = %d\n", fd);

	//恢复串口为阻塞状态
    if(fcntl(fd, F_SETFL, 0) < 0) {  
        ALOGE("fcntl failed!\n");  
		return -1;
	} else {  
		ALOGE("fcntl=%d\n",fcntl(fd, F_SETFL,0));  
    }  

	//测试是否为终端设备
	/*
	if(0 == isatty(STDIN_FILENO)) {  
        ALOGE("standard input is not a terminal device\n");  
		return -1;
	} else {  
		ALOGE("isatty success!\n");  
	} */     

    if (serial_set(fd,9600,0,8,1,'N') == -1) {
		ALOGE("%s: serial_set failed\n", __func__);
		return -1;
	}

	return fd;

#if 0
	tcgetattr(fd, &opt);
	//tcflush(fd, TCIOFLUSH);
	cfsetispeed(&opt, B9600);
	cfsetospeed(&opt, B9600);

	//tcflush(fd, TCIOFLUSH);

	opt.c_cflag |= (CLOCAL | CREAD);

	opt.c_cflag &= ~CSIZE;
	opt.c_cflag &= ~CRTSCTS;
	opt.c_cflag |= CS8;

	/* 
	   opt.c_cflag |= PARENB;  // 
	   opt.c_cflag |= PARODD;  //
	   opt.c_iflag |= (INPCK | ISTRIP);  // 
	   */ 
	//none parity
	opt.c_iflag |= IGNPAR;

	//stopbit
	opt.c_cflag &= ~CSTOPB;

	opt.c_oflag = 0;
	opt.c_lflag = 0;

	tcsetattr(fd, TCSANOW, &opt);

	return fd;
#endif
}

static int serial_device_open(const struct hw_module_t* module, const char* name,
		struct hw_device_t** device)
{
	ALOGD("%s E", __func__);
	serial_control_device_t *dev;
	//struct termios opt; 

	dev = (serial_control_device_t *)malloc(sizeof(*dev));
	memset(dev, 0, sizeof(*dev)); 

	//HAL must init property
	dev->common.tag= HARDWARE_DEVICE_TAG;
	dev->common.version = 0;
	dev->common.module= module;
	dev->common.close= serial_device_close;

	dev->serial_control_info = serial_control_info;

	*device= &dev->common;

	ctrl_fd = base_serial_device_open("/dev/ttyMT0");
	if(ctrl_fd < 0) {
		ALOGE("%s: /dev/ttyMT0 open: %s\n", __func__, strerror(errno));
		return -1;
	}

	ALOGD("%s X", __func__);

	return 0;
}

static struct hw_module_methods_t serial_module_methods = {
	open: serial_device_open  
};

serial_module_t HAL_MODULE_INFO_SYM = {
    common: {
        tag: HARDWARE_MODULE_TAG,
        version_major: 0,
        version_minor: 1,
        id: RSERIAL_HARDWARE_MODULE_ID,
        name: "ramos serial module",
        author: "The Android Open Source Project",
        methods: &serial_module_methods,
    }
	/* supporting APIs go here */
};


