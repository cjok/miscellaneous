
#include <hardware/hardware.h>
#include <fcntl.h>
#include <errno.h>
#include <cutils/log.h>
#include <cutils/atomic.h>

#define RSERIAL_HARDWARE_MODULE_ID "rserial"

//control module function
#define TYPE_CONTROL_GET_SPEED_CONFIG	0
#define TYPE_CONTROL_SET_SPEED_CONFIG	1
#define TYPE_CONTROL_GET_CONTROL_STATUS	2
#define TYPE_CONTROL_GET_TRAFFIC_STATUS	3
#define TYPE_CONTROL_SYNC_STATUS	4

typedef struct serial_module {
	struct hw_module_t common;
}serial_module_t;


typedef struct serial_control_device {
	struct hw_device_t common; 

	/* supporting control APIs go here */
	int (*serial_control_info)(struct serial_control_device *dev, int type,
		int data1, int data2, int *buf);

/*
	int (*serial_set_control_speed)(struct serial_control_device_t *dev, int type,
		int limit_speed, int mode);

	int (*serial_control_sync_status)(struct serial_control_device_t *dev, int type,
		int status, int angle);
*/
} serial_control_device_t;

#define MAX_RECV_BUF_SIZE	24	

//control module data packet
struct ctrl_serial_packet {
	unsigned char start;
	unsigned char command;
	unsigned char length;
	unsigned char data_buf[MAX_RECV_BUF_SIZE];
	unsigned char check_sum;
	unsigned char end;
};

struct ctrl_status {
	unsigned int status;
	unsigned int voltage;
	unsigned int current;
	unsigned int temp;
	unsigned int rotate_speed;
	unsigned int mileage;
};



static unsigned char cmd_line[24] = {'\0'};












