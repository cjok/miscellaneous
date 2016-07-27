
#include <hardware/hardware.h>
#include <fcntl.h>
#include <errno.h>
#include <cutils/log.h>
#include <cutils/atomic.h>

#define SBM_HARDWARE_MODULE_ID "smart_battery"

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


typedef struct sbm_module {
	struct hw_module_t common;
} sbm_module_t;


typedef struct sbm_device {
	struct hw_device_t device; 

	/* supporting control APIs go here */
	int (*sbm_ioctl)(struct sbm_device *dev, int cmd, void *arg);

} sbm_device_t;



