#ifndef _SMART_BATTERY_H_
#define _SMART_BATTERY_H_ 
	 
#include <linux/ioctl.h>
#include <linux/miscdevice.h>
#include <linux/i2c.h>
#include <linux/mutex.h>

#define SBM_DEBUG
#ifdef SBM_DEBUG
#define pr_debug(fmt, arg...)		\
			printk(KERN_ERR "[sbm]%s@%d " fmt, __func__, __LINE__, ##arg)
#else
#define pr_debug(fmt, arg...)	do{}while(0)
#endif

#define SBM_DEV_NAME	"sbm-smbus"

#define MTK_PLATFORM

#define MAX_RECV_BUF_SIZE	16

//IOCTL commands
#define SIMPLE_MAGIC	's'
#define TYPE_GET_BATTERY_TEMP		0x08
#define TYPE_GET_BATTERY_VOLTAGE	0x09
#define TYPE_GET_BATTERY_CURRENT	0x0a
#define TYPE_GET_REL_SOC			0x0d
#define TYPE_GET_ABS_SOC			0x0e
#define TYPE_GET_REMAIN_CAPACITY	0x0f
#define TYPE_GET_FULL_CAPACITY		0x10
#define TYPE_GET_CYCLE_COUNTS		0x17
#define TYPE_GET_SINGLE_BATTERY_VOLTAGE_1_7		0xA0
#define TYPE_GET_SINGLE_BATTERY_VOLTAGE_8_14	0xA1
#define TYPE_GET_SINGLE_BATTERY_VOLTAGE			0xA2


//struct sbm_device {
//	struct miscdevice mdev;
//	struct mutex mutex;
//	struct i2c_client *client;
//};


#endif

