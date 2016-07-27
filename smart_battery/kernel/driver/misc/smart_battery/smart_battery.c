/*
* smbus smart battery manage driver
*
* i2c command format
* slave addr | command | slave addr |  data   |  CRC
*  1 byte       1 byte    1 byte       n*byte   1 byte
*/
#include <linux/interrupt.h>
#include <linux/slab.h>
#include <linux/irq.h>
#include <asm/uaccess.h>
#include <linux/delay.h>
#include <linux/input.h>
#include <linux/workqueue.h>
#include <linux/kobject.h>
#include <linux/earlysuspend.h>
#include <linux/platform_device.h>
#include <asm/atomic.h>
#include <linux/ioctl.h>
#include <linux/wakelock.h>

#include <mach/mt_pm_ldo.h>

#include "smart_battery.h"
#if 0
static struct i2c_board_info __initdata sbm_i2c_board_info[] = {
	{
		I2C_BOARD_INFO(SBM_DEV_NAME, 0xb),
	},
};
#endif

static int sbm_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id);
static int sbm_i2c_remove(struct i2c_client *client);
#ifndef CONFIG_HAS_EARLYSUSPEND
static int sbm_suspend(struct i2c_client *client, pm_message_t msg);
static int sbm_resume(struct i2c_client *client);
#endif

static const struct i2c_device_id sbm_i2c_id[] = {{SBM_DEV_NAME, 0}, {}};

static struct i2c_client *sbm_i2c_client = NULL;

static struct of_device_id sbm_match_table[] = {
	{.compatible = "mediatek,sbm",},
	{},
};

static struct i2c_driver sbm_i2c_driver = {
    .driver = {
        .name = SBM_DEV_NAME,
		.of_match_table = sbm_match_table,
    },
    .probe  = sbm_i2c_probe,
    .remove = sbm_i2c_remove,

#if !defined(CONFIG_HAS_EARLYSUSPEND)
    .suspend = sbm_suspend,
    .resume  = sbm_resume,
#endif

    .id_table = sbm_i2c_id,
};


static int sbm_i2c_read(struct i2c_client *client, u8 addr, u8 *data, u8 len)
{
    int ret;
    struct i2c_msg msgs[2] = {
		{
			.addr = client->addr,
			.flags = I2C_M_NOSTART,
			.len = 1,
			.buf = &addr,
		},
		{
			.addr = client->addr,
			.flags = I2C_M_RD,
			.len = len,
			.buf = data,
		}
	};

    if (!client) {
        pr_debug("client is NULL!\n");
        return -EINVAL;
    }

    ret = i2c_transfer(client->adapter, msgs, sizeof(msgs)/sizeof(msgs[0]));

    return (ret == 2) ? len : ret;
}

static int sbm_i2c_write(struct i2c_client *client, u8 addr, u8 *data, u8 len)
{  
	u8 buf[2];
	int ret;
	struct i2c_msg msgs[1] = {
		{
			.addr = client->addr,
			.flags = 0,
			.len = len + 1,
			.buf = buf,
		}
	};

    if (!client) {
        pr_debug("cleint is NULL!\n");
        return -EINVAL;
    }

	buf[0] = addr;
	memcpy(&buf[1], data, len);

    ret = i2c_transfer(client->adapter, msgs, 1);

	pr_debug("ret = %d\n", ret);

   	return (ret == 1) ? sizeof(buf) : ret;
}

static int sbm_open(struct inode *inode, struct file *file)
{

    file->private_data = sbm_i2c_client;

    if (NULL == file->private_data) {
        pr_debug("null pointer!!\n");
        return -EINVAL;
    }
    return nonseekable_open(inode, file);
}

static int sbm_release(struct inode *inode, struct file *file)
{
    file->private_data = NULL;
    return 0;
}

static long sbm_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
	int err = 0;
	void __user *data = NULL;
	u8 buf[16];
	int vol_buf[14];
	int result;
	int i = 0;

    struct i2c_client *client = (struct i2c_client*)file->private_data;
	//struct sbm_device *sd = (struct sbm_device *)i2c_get_clientdata(client);

    if (_IOC_DIR(cmd) & _IOC_READ)
        err = !access_ok(VERIFY_WRITE, (void __user *)arg, _IOC_SIZE(cmd));

    if (_IOC_DIR(cmd) & _IOC_WRITE)
        err = !access_ok(VERIFY_READ, (void __user *)arg, _IOC_SIZE(cmd));

    if (err) {
        pr_debug("access error: %08X, (%2d, %2d)\n", cmd, 
			_IOC_DIR(cmd), _IOC_SIZE(cmd));
        return -EFAULT;
	}

	memset(buf, 0, sizeof(buf));
	memset(vol_buf, 0, sizeof(vol_buf));

    switch (cmd)
    {
		case TYPE_GET_BATTERY_TEMP:
		case TYPE_GET_BATTERY_VOLTAGE:
		case TYPE_GET_BATTERY_CURRENT:
		case TYPE_GET_REMAIN_CAPACITY:
		case TYPE_GET_FULL_CAPACITY:
		case TYPE_GET_CYCLE_COUNTS:
            pr_debug("%x \n", cmd);
	#if 1
			err = sbm_i2c_read(client, cmd, buf, 3);
			if (err != 3) {
                err = -EINVAL;
				pr_debug("len = %d\n", err);
                break;
			}

			result = buf[0] | (buf[1] << 8);
	#endif
			err = __put_user(result, (int __user *)arg);

            break;

		case TYPE_GET_REL_SOC:
		case TYPE_GET_ABS_SOC:
	 	     pr_debug("%x \n", cmd);
	#if 1
			err = sbm_i2c_read(client, cmd, buf, 2);
			if (err != 2) {
                err = -EINVAL;
				pr_debug("len = %d\n", err);
                break;
			}

			result = buf[0];
	#endif
			err = __put_user(result, (int __user *)arg);

			break;

		case TYPE_GET_SINGLE_BATTERY_VOLTAGE:
			err = sbm_i2c_read(client, TYPE_GET_SINGLE_BATTERY_VOLTAGE_1_7, buf, 15);
			if (err != 15) {
                err = -EINVAL;
				pr_debug("len = %d\n", err);
                break;
			}
			
			vol_buf[0] = buf[0] | (buf[1] << 8);
			vol_buf[1] = buf[2] | (buf[3] << 8);
			vol_buf[2] = buf[4] | (buf[5] << 8);
			vol_buf[3] = buf[6] | (buf[7] << 8);
			vol_buf[4] = buf[8] | (buf[9] << 8);
			vol_buf[5] = buf[10] | (buf[11] << 8);
			vol_buf[6] = buf[12] | (buf[13] << 8);

			memset(buf, 0, sizeof(buf));
			err = sbm_i2c_read(client, TYPE_GET_SINGLE_BATTERY_VOLTAGE_8_14, buf, 13);
			if (err != 13) {
                err = -EINVAL;
				pr_debug("len = %d\n", err);
                break;
			}
			
			vol_buf[7] = buf[0] | (buf[1] << 8);
			vol_buf[8] = buf[2] | (buf[3] << 8);
			vol_buf[9] = buf[2] | (buf[3] << 8);
			vol_buf[10] = buf[4] | (buf[5] << 8);
			vol_buf[11] = buf[6] | (buf[7] << 8);
			vol_buf[12] = buf[8] | (buf[9] << 8);
//			vol_buf[13] = buf[10] | (buf[11] << 8);

			if (copy_to_user((void __user *)arg, vol_buf, 13)) {
                err = -EFAULT;
				pr_debug("copy_to_user err = %d\n", err);
            }
			break;

        default:
            pr_debug("unknown IOCTL: 0x%08x\n", cmd);
            err = -ENOIOCTLCMD;
            break;
    }

    return err;
}

static struct file_operations sbm_fops = {
    .owner          = THIS_MODULE,
    .open           = sbm_open,
    .release        = sbm_release,
    .unlocked_ioctl = sbm_ioctl,
#ifdef CONFIG_COMPAT
//	.compat_ioctl = sbm_compat_ioctl,
#endif                                               
};

static struct miscdevice sbm_dev = {
	.minor = MISC_DYNAMIC_MINOR,
	.name = "smart_battery",
	.fops = &sbm_fops,
};

#ifndef CONFIG_HAS_EARLYSUSPEND
static int sbm_suspend(struct i2c_client *client, pm_message_t msg)
{
//    struct sbm_i2c_data *obj = i2c_get_clientdata(client);
	return 0;
 }

static int sbm_resume(struct i2c_client *client)
{
//    struct sbm_i2c_data *obj = i2c_get_clientdata(client);
     return 0;
}
#else
static void sbm_early_suspend(struct early_suspend *h)
{
}

static void sbm_late_resume(struct early_suspend *h)
{
//    struct sbm_i2c_data *obj = container_of(h, struct mc3xxx_i2c_data, early_drv);
 }
#endif

static int is_device_alive(struct i2c_client *client)
{
	int ret = -1;
	u8 buf[8];
	int i;
	
	ret = sbm_i2c_read(client, TYPE_GET_BATTERY_VOLTAGE, buf, 3);
	
	pr_debug("buf[0] = 0x%x, buf[1] = 0x%x, buf[2] = 0x%x, ret = %d\n", 
		buf[0], buf[1], buf[2], ret);

	if (ret <= 0) {
		pr_debug("i2c communication failed, ret = %d\n", ret);
		return -1;
	}

	return 0;
}

static int sbm_read_single_status(struct i2c_client *client, int type, int len)
{
	int ret = -1;
	u8 buf[MAX_RECV_BUF_SIZE];
	int i;
	
	ret = sbm_i2c_read(client, type, buf, len);
	
	pr_debug("type: 0x%x, ret = %d, buf: ", type, ret);
	if (ret <= 0) {
		pr_debug("i2c communication failed, ret = %d\n", ret);
		return -1;
	}

	for (i = 0; i < ret; i++) {
		printk("0x%x ", buf[i]);
	}
	printk("\n");

	return 0;
}

static int sbm_read_all_status(struct i2c_client *client)
{
	sbm_read_single_status(client, TYPE_GET_BATTERY_TEMP, 3);
	sbm_read_single_status(client, TYPE_GET_BATTERY_VOLTAGE, 3);
	sbm_read_single_status(client, TYPE_GET_BATTERY_CURRENT, 3);
	sbm_read_single_status(client, TYPE_GET_REL_SOC, 2);
	sbm_read_single_status(client, TYPE_GET_ABS_SOC, 2);
	sbm_read_single_status(client, TYPE_GET_REMAIN_CAPACITY, 3);
	sbm_read_single_status(client, TYPE_GET_FULL_CAPACITY, 3);
	sbm_read_single_status(client, TYPE_GET_CYCLE_COUNTS, 3);
	sbm_read_single_status(client, TYPE_GET_SINGLE_BATTERY_VOLTAGE_1_7, 15);
	sbm_read_single_status(client, TYPE_GET_SINGLE_BATTERY_VOLTAGE_8_14, 15);

	return 0;
}

static int sbm_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id)
{
    int err = -1;

	pr_debug("enter\n");

#ifdef MTK_PLATFORM
	if(TRUE != hwPowerOn(MT6328_POWER_LDO_VCAM_IO, VOL_1800, "smart_battery")) {
		pr_debug("power on VCAM_IO failed\n");
		return -1;
	}
#endif

	mdelay(10);

	//sbm_read_all_status(client);

	err = is_device_alive(client);
	if (err) {
		goto err_i2c;
	}

	sbm_i2c_client  = client;

//	i2c_set_clientdata(client, sd);

	err = misc_register(&sbm_dev);
    if (err) {
        pr_debug("sbm_dev register failed\n");
        goto err_i2c;
    }

	pr_debug("ok\n");

    return 0;

err_i2c:
#ifdef MTK_PLATFORM
	hwPowerDown(MT6328_POWER_LDO_VCAM_IO, "smart_battery");
#endif

    return err;
}

static int sbm_i2c_remove(struct i2c_client *client)
{
    misc_deregister(&sbm_dev);
#ifdef MTK_PLATFORM
	hwPowerDown(MT6328_POWER_LDO_VCAM_IO, "smart_battery");
#endif

    return 0;
}

static int __init sbm_init(void)
{
	int ret;

	pr_debug("enter \n");

#if 0
    ret = i2c_register_board_info(2, sbm_i2c_board_info, ARRAY_SIZE(sbm_i2c_board_info));
	if (ret) {
		pr_debug("ret = %d\n", ret);
	}
#endif

    if (i2c_add_driver(&sbm_i2c_driver)) {
		pr_debug("i2c add driver failed");
		return -1;
	}
	pr_debug("exit\n");

    return 0;
}

static void __exit sbm_exit(void)
{
    i2c_del_driver(&sbm_i2c_driver);
	pr_debug("\n");
}

module_init(sbm_init);
module_exit(sbm_exit);

MODULE_DESCRIPTION("smbus smart battery driver v1.0");
MODULE_AUTHOR("liaoye");
MODULE_LICENSE("GPL");
