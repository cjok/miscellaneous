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

#include <hardware/smart_battery.h>

#define LOG_TAG		"xxsmart_battery"

static int sbm_fd;	//control module fd

static int sbm_device_close(struct hw_device_t* device)
{
	ALOGD("%s E", __func__);
	sbm_device_t* ctx = (sbm_device_t*)device;
	if (ctx) {
		free(ctx);
	}
	close(sbm_fd);
	ALOGD("%s X", __func__);
	return 0; 
}

static int base_device_open(const char *dev_path)
{
	int fd;

	fd = open(dev_path, O_RDWR);
	if(fd < 0) {
		ALOGE("%s: %s open: %s\n", __func__, dev_path, strerror(errno));
		return -1;
	}

	ALOGE("fd = %d\n", fd);
	
	return fd;

}

static int sbm_ioctl(struct sbm_device *dev, int cmd, void *arg)
{
	int ret = -1;
	int *result = (int *)arg;

	switch (cmd) {
		case TYPE_GET_BATTERY_TEMP:
		case TYPE_GET_BATTERY_VOLTAGE:
		case TYPE_GET_BATTERY_CURRENT:
		case TYPE_GET_REL_SOC:
		case TYPE_GET_ABS_SOC:
		case TYPE_GET_REMAIN_CAPACITY:
		case TYPE_GET_FULL_CAPACITY:
		case TYPE_GET_CYCLE_COUNTS:
		case TYPE_GET_SINGLE_BATTERY_VOLTAGE:
			ret = ioctl(sbm_fd, cmd, result);
			if (ret) {
				ALOGE("%s: ioctl cmd = %x failed: %s\n", __func__, 
					cmd, strerror(errno));
			}
			break;

		default:
			ALOGE("ioctl unsupport cmd = %d\n", cmd);
			ret = -1;
			break;
	}

	return ret;
}

static int sbm_device_open(const struct hw_module_t* module, const char* name,
		struct hw_device_t** device)
{
	ALOGD("%s E", __func__);
	sbm_device_t *dev;

	dev = (sbm_device_t *)malloc(sizeof(*dev));
	memset(dev, 0, sizeof(*dev)); 

	//HAL must init property
	dev->device.tag= HARDWARE_DEVICE_TAG;
	dev->device.version = 0;
	dev->device.module= module;
	dev->device.close = sbm_device_close;

	//device 所包含的方法
	dev->sbm_ioctl = sbm_ioctl;

	//赋值给device，让jni拿到device,从而获取到dev中的方法
	*device= &dev->device;

	sbm_fd = base_device_open("/dev/smart_battery");

	if(sbm_fd < 0) {
		ALOGE("%s: /dev/smart_battery open: %s\n", __func__, strerror(errno));
		free(dev);
		return -1;
	}

	ALOGD("%s X", __func__);

	return 0;
}

static struct hw_module_methods_t sbm_module_methods = {
	open: sbm_device_open  
};

sbm_module_t HAL_MODULE_INFO_SYM = {
    common: {
        tag: HARDWARE_MODULE_TAG,
        version_major: 0,
        version_minor: 1,
        id: SBM_HARDWARE_MODULE_ID,
        name: "ramos smart battery module",
        author: "The Android Open Source Project",
        methods: &sbm_module_methods,
    }
	/* supporting APIs go here */
};


