/*! \file
*  
*  \brief  pm_app_smbchg_alg.c
*  \details Implementation file for pmic sbl charging algorithm
*    
*  &copy; Copyright 2013 QUALCOMM Technologies Incorporated, All Rights Reserved
*/

/*===========================================================================
                                Edit History
This document is created by a code generator, therefore this section will
not contain comments describing changes made to the module.

$Header: //components/rel/boot.bf/3.3/boot_images/core/systemdrivers/pmic/app/chg/src/pm_app_smbchg.c#7 $
$DateTime: 2016/05/23 02:20:49 $
$Author: pwbldsvc $
 
when       who     what, where, why
--------   ---     ---------------------------------------------------------- 
03/11/16   pxm     add apsd re-run function to fix the bug that detect DCP as SDP. CR834431
09/16/15   pxm     Updated pm_smbchg_bat_if_config_chg_cmd() replacing to pm_smbchg_chgr_set_chgr_sts() 
07/24/15   pxm     Remove IUSB 100ma limit after exiting SBL charging
06/30/15   pxm     Abstract fuction for ADC reading. Fix issue that unplug battery during charging lead to crash. CR846814
02/22/15   aab     Updated pm_sbl_chg_check_weak_battery_status() to support wipower
02/12/15   aab     Added Multiple try to read ADC READ is Ready
11/18/14   aab     Updated SBL charger driver to use Vbatt ADC
11/16/14   aab     Disabled log messages
10/15/14   aab     Added pm_sbl_config_chg_parameters() 
10/14/14   aab     Added pm_sbl_config_fg_sram(). Updated pm_sbl_chg_config_vbat_low_threshold()
08/20/14   aab     Updated to get DBC bootup threshold voltage from Dal Config.  Enabled DBC.
06/24/14   aab     Updated pm_sbl_chg_check_weak_battery_status() to include RED LED blinking
04/28/14   aab     Creation
===========================================================================*/

/*===========================================================================

                     INCLUDE FILES 

===========================================================================*/
#include "com_dtypes.h"
#include "pm_app_smbchg.h"
#include "boothw_target.h"
#include "pm_smbchg_chgr.h"
#include "pm_smbchg_bat_if.h"
#include "pm_app_smbchg.h"
#include "boot_api.h"
#include "pm_utils.h"
#include "pm_rgb.h"
#include "pm_target_information.h"
#include "pm_fg_sram.h"
#include "pm_fg_adc_usr.h"
#include "pm_comm.h"
#include "CoreVerify.h"
#include "boot_logger.h"
#include "boot_logger_timer.h"


/*===========================================================================

                     PROTOTYPES 

===========================================================================*/


/*=========================================================================== 

                     GLOBAL TYPE DEFINITIONS

===========================================================================*/
//#define  PM_REG_CONFIG_SETTLE_DELAY       175  * 1000 //175ms  ; Delay required for battery voltage de-glitch time
#define  PM_WEAK_BATTERY_CHARGING_DELAY   500 * 1000  //500ms
#define  PM_WIPOWER_START_CHARGING_DELAY  3500 * 1000 //3.5sec
#define  PM_MIN_ADC_READY_DELAY             1 * 1000  //1ms
#define  PM_MAX_ADC_CHECK_TIMES     2000              //2s
#define SBL_PACKED_SRAM_CONFIG_SIZE 3

#define LOG_BUF_SIZE 128

static char s_buf[LOG_BUF_SIZE] = {0};
static boolean s_log_on = TRUE;

// LOGD: debug logging. Enable by setting s_log_on TRUE, disable by setting s_log_on FALSE.
// We should by default disable logging it since it would increase boot time
#define LOGD(x) \
    do { \
        if(s_log_on) { \
            boot_log_message(x); \
        } \
    } while(0)

// leave here so we can use it directly when we need to debug in this file.
#define LOGD_STUB(x) \
    do { \
        if(s_log_on) { \
            snprintf(s_buf, LOG_BUF_SIZE, "%s. Func: %s, L%d", x, __FUNCTION__, __LINE__);\
            boot_log_message(s_buf); \
        } \
    } while(0)

// Usually such kind of failure will lead to error fatal in SBL1, so output the error info would help.
// Delay is used to make sure log been flushed.
#define CHG_VERIFY(rc) \
    do {\
        if(PM_ERR_FLAG__SUCCESS != rc) \
        {\
            snprintf(s_buf, LOG_BUF_SIZE, "func, %s, line, %d, rc: %d", __FUNCTION__, __LINE__, rc); \
            boot_log_message(s_buf); \
            pm_clk_busy_wait(10 * 1000); \
            return rc;\
        }\
    }while(0)

static pm_smbchg_bat_if_low_bat_thresh_type pm_dbc_bootup_volt_threshold;

pm_err_flag_type pm_sbl_chg_no_battery_chgr_detection(uint32 device_index, boolean dbc_usb_500_mode);

/*===========================================================================

                     FUNCTION IMPLEMENTATION 

===========================================================================*/

pm_err_flag_type pm_sbl_chg_get_vbat(uint8 device_index, uint32* vbat_adc)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
    boolean adc_reading_ready = FALSE;
    uint16  wait_index = 0;

    //Check if Vbatt ADC is Ready
    for (wait_index = 0; wait_index < PM_MAX_ADC_CHECK_TIMES; wait_index++)
    {
        CHG_VERIFY(pm_fg_adc_usr_get_bcl_values(device_index,&adc_reading_ready));
        if(!adc_reading_ready)
        {
            CHG_VERIFY(pm_clk_busy_wait(PM_MIN_ADC_READY_DELAY));
        }
        else
        {
            break;
        }
    }

    if(adc_reading_ready) 
    {
        CHG_VERIFY(pm_fg_adc_usr_get_calibrated_vbat(device_index, vbat_adc)); //Read calibrated vbatt ADC
        snprintf(s_buf, LOG_BUF_SIZE, "vbat: %d", *vbat_adc);
        boot_log_message(s_buf);
    }
    else
    {
        boot_log_message("ERROR:  ADC Reading is NOT Ready");
        err_flag = PM_ERR_FLAG__ADC_NOT_READY;
    }

    return err_flag;
}

pm_err_flag_type pm_smbchg_reset_apsd(uint32 device_index)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  
    err_flag |= pm_smbchg_usb_chgpth_set_usbin_adptr_allowance(device_index, PM_SMBCHG_USBIN_ADPTR_ALLOWANCE_9V);
    err_flag |= pm_smbchg_usb_chgpth_set_usbin_adptr_allowance(device_index, PM_SMBCHG_USBIN_ADPTR_ALLOWANCE_5V_TO_9V);
    
	return err_flag;
}

pm_err_flag_type pm_sbl_chg_check_weak_battery_status(uint32 device_index)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
    pm_smbchg_specific_data_type *chg_param_ptr = NULL;
    pm_smbchg_chgr_status_type vbatt_chging_status;
	pm_smbchg_bat_if_low_bat_thresh_type  vlow_bat_threshold;
    boolean hot_bat_hard_lim_rt_sts  = FALSE;
    boolean cold_bat_hard_lim_rt_sts = FALSE;
    boolean vbatt_weak_status = TRUE;
    boolean toggle_led = FALSE;
    boolean bat_present = TRUE;
	uint8   count;
    uint32  vbat_adc = 0;
    uint32  bootup_threshold = 0;
	uint32  apsd_reset_delta = 0;
    uint32  vlow_bat_value = 0;
    boolean vbatt_status = FALSE;
    boolean configure_icl_flag = FALSE;
    boolean charger_present = FALSE;
	boolean apsd_reset_flag = FALSE;
    // pm_smbchg_usb_chgpth_pwr_pth_type charger_path = PM_SMBCHG_USB_CHGPTH_PWR_PATH__INVALID;
    pm_smbchg_misc_src_detect_type chgr_src_detected = PM_SMBCHG_MISC_SRC_DETECT_INVALID;

    boolean first_cycle = TRUE; // the first 500ms cycle

    //boot_log_message("BEGIN:  PMIC SBL Weak Battery Status Check");
    chg_param_ptr = (pm_smbchg_specific_data_type*)pm_target_information_get_specific_info(PM_PROP_SMBCHG_SPECIFIC_DATA);
    CORE_VERIFY_PTR(chg_param_ptr);
    bootup_threshold = chg_param_ptr->bootup_battery_theshold_mv; 
	pm_smbchg_bat_if_get_low_bat_volt_threshold(device_index, &vlow_bat_threshold, &vlow_bat_value);

    if(chg_param_ptr->dbc_bootup_volt_threshold.enable_config == PM_ENABLE_CONFIG)
    {
        //Configure Vlowbatt threshold: Used by PMI on next bootup
        CHG_VERIFY(pm_sbl_chg_config_vbat_low_threshold(device_index, chg_param_ptr));
    }

    //Check Battery presence
    CHG_VERIFY(pm_smbchg_bat_if_get_bat_pres_status(device_index, &bat_present));
    if( !bat_present )
    {
        //WA for Booting with NO Battery/SDP
        err_flag  |= pm_sbl_chg_no_battery_chgr_detection(device_index, chg_param_ptr->dbc_usb_500_mode);
        LOGD("Booting up to HLOS: Charger is Connected and NO battery");
        err_flag |= pm_smbchg_bat_if_set_min_sys_volt(device_index, 3600); // Set Vsysmin to 3.6V if battery absent
        return err_flag;
    }

    bootup_threshold = chg_param_ptr->bootup_battery_theshold_mv;
	apsd_reset_delta = chg_param_ptr->apsd_reset_theshold_mv.apsd_reset_delta_mv;

    CHG_VERIFY(pm_smbchg_usb_chgpth_set_usbin_adptr_allowance(device_index, PM_SMBCHG_USBIN_ADPTR_ALLOWANCE_5V_TO_9V));

    //Enable BMS FG Algorithm BCL
    CHG_VERIFY(pm_fg_adc_usr_enable_bcl_monitoring(device_index, TRUE));
	
	//Check if USB charger is SDP
    CHG_VERIFY(pm_smbchg_misc_chgr_port_detected(device_index, &chgr_src_detected));
    if (chgr_src_detected == PM_SMBCHG_MISC_SRC_DETECT_SDP) 
    {
        if (!configure_icl_flag)
        {
            //Check Vlow_batt status
            CHG_VERIFY(pm_smbchg_chgr_vbat_sts(device_index, &vbatt_status));
            if (vbatt_status)
            {
                 //set ICL to 500mA
                 boot_log_message("Manually set to USB500 mode");
                 CHG_VERIFY(pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__USB51_MODE, TRUE));
                 CHG_VERIFY(pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__USBIN_MODE_CHG, FALSE));
                 configure_icl_flag = TRUE;
            }
        }
	}

    while(vbatt_weak_status)  //While battery is in weak state
    {
        CHG_VERIFY(pm_sbl_chg_get_vbat(device_index, &vbat_adc));
        //Check if ADC reading is within limit
        if (vbat_adc >=  bootup_threshold)  //Compare it with SW bootup threshold
        {
            LOGD("Battery good, bootup");
            vbatt_weak_status = FALSE;
            break; //bootup
        }
        if(first_cycle)//check in lk by lijian
        {
            LOGD("force bootup");
            break; //bootup
        }

        //Check if USB charger is SDP
        if (chgr_src_detected == PM_SMBCHG_MISC_SRC_DETECT_SDP) 
        {
			//Reset APSD once, if Vbatt >= apsd reset theshold
            if (chg_param_ptr->apsd_reset_theshold_mv.enable_config == PM_ENABLE_CONFIG)
            {
                
                if ( (apsd_reset_flag == FALSE) && ( vbat_adc >= vlow_bat_value + apsd_reset_delta) )
                {
                    boot_log_message("APSD Reset Start");

                    err_flag |= pm_smbchg_usb_chgpth_config_aicl(device_index, PM_SMBCHG_USB_CHGPTH_AICL_CFG__AICL_EN, FALSE); //Disable AICL
                    err_flag |= pm_smbchg_reset_apsd(device_index);                                                            
                    err_flag |= pm_smbchg_usb_chgpth_config_aicl(device_index, PM_SMBCHG_USB_CHGPTH_AICL_CFG__AICL_EN, TRUE);  //Enable AICL
                    apsd_reset_flag = TRUE;
 
                    for (count = 0; count < 10; count++)
                    {
                        err_flag |= pm_smbchg_misc_chgr_port_detected(device_index, &chgr_src_detected);
                        if (chgr_src_detected != PM_SMBCHG_MISC_SRC_DETECT_INVALID)
                        {
                            break;
                        }
                        err_flag |= pm_clk_busy_wait(100 * 1000); //100ms 
                    }
                    boot_log_message("APSD Reset Done");
                 }
			} 
        }

        //Check if JEITA check is enabled
        if (chg_param_ptr->enable_jeita_hard_limit_check)
        {
            //Read JEITA condition
            CHG_VERIFY(pm_smbchg_bat_if_irq_status(device_index, PM_SMBCHG_BAT_IF_HOT_BAT_HARD_LIM,  PM_IRQ_STATUS_RT, &hot_bat_hard_lim_rt_sts ));
            CHG_VERIFY(pm_smbchg_bat_if_irq_status(device_index, PM_SMBCHG_BAT_IF_COLD_BAT_HARD_LIM, PM_IRQ_STATUS_RT, &cold_bat_hard_lim_rt_sts));

            if ( hot_bat_hard_lim_rt_sts || cold_bat_hard_lim_rt_sts )  
            {
                boot_log_message("Keep in loop due to JEITA limit");
                continue;  // Stay in this loop as long as JEITA Hard Hot/Cold limit is exceeded
            }
        }

        //Toggle Red LED and delay 500ms
        toggle_led = !toggle_led;
        //      err_flag |= pm_rgb_led_config(device_index, PM_RGB_1, PM_RGB_SEGMENT_R,  PM_RGB_VOLTAGE_SOURCE_VPH, PM_RGB_DIM_LEVEL_LOW, toggle_led);
        CHG_VERIFY(pm_clk_busy_wait(PM_WEAK_BATTERY_CHARGING_DELAY)); //500ms 

        //Check if Charging in progress
        CHG_VERIFY(pm_smbchg_chgr_get_chgr_sts(device_index, &vbatt_chging_status));

        if ( vbatt_chging_status.charging_type == PM_SMBCHG_CHGR_NO_CHARGING )
        {
            LOGD("====== no charging ========");
            CHG_VERIFY(pm_smbchg_chager_active(device_index, &charger_present));
            if(first_cycle && charger_present)
            {
                // For the case charging is disabled by default. Need to explicitly enable
                if(!vbatt_chging_status.charger_enable)
                {
                    // charging is disabled, explicitly enable it.
                    boot_log_message("Explicitly enable charging");
                    //Ensure that Charging is enabled
                    CHG_VERIFY(pm_smbchg_chgr_enable_src(device_index, FALSE));
                    CHG_VERIFY(pm_smbchg_chgr_set_chg_polarity_low(device_index, FALSE));
                    CHG_VERIFY(pm_smbchg_bat_if_config_chg_cmd(device_index, PM_SMBCHG_BAT_IF_CMD__EN_BAT_CHG, TRUE));
                    CHG_VERIFY(pm_clk_busy_wait(PM_WEAK_BATTERY_CHARGING_DELAY));
                }
                else
                {
                    boot_log_message("Charging by default enabled");
                }
            }
            else
            {
                if(!charger_present)
                {
                    boot_log_message("charger absent");
                }
                boot_log_message("Charging is NOT in progress for weak battery: Shutting Down");
                if(s_log_on)
                {
                    CHG_VERIFY(pm_clk_busy_wait(10 * 1000));  // Add 10ms for output log. Used for debugging
                }
                boot_hw_powerdown();
            }
        }
        if(first_cycle)
        {
            first_cycle = FALSE;
        }
    }//while

    

    toggle_led = FALSE;
    //   err_flag |= pm_rgb_led_config(device_index, PM_RGB_1, PM_RGB_SEGMENT_R,  PM_RGB_VOLTAGE_SOURCE_VPH, PM_RGB_DIM_LEVEL_LOW, toggle_led);

    if(!bat_present)
    {
        // If battery absent, set Vsysmin to 3.6V
        err_flag |= pm_smbchg_bat_if_set_min_sys_volt(device_index, 3600);
    }
    else
    {
        // If battery present, set Vsysmin to 3.15V
        err_flag |= pm_smbchg_bat_if_set_min_sys_volt(device_index, 3150);
    }

    return err_flag; 
}

pm_err_flag_type pm_sbl_chg_config_vbat_low_threshold(uint32 device_index, pm_smbchg_specific_data_type *chg_param_ptr)
{
   pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
   
   pm_dbc_bootup_volt_threshold = chg_param_ptr->dbc_bootup_volt_threshold.vlowbatt_threshold;

   if (chg_param_ptr->dbc_bootup_volt_threshold.enable_config == PM_ENABLE_CONFIG)
   {
      if (pm_dbc_bootup_volt_threshold  >= PM_SMBCHG_BAT_IF_LOW_BATTERY_THRESH_INVALID)
      {
         err_flag = PM_ERR_FLAG__INVALID_VBATT_INDEXED;
         return err_flag;
      }

      err_flag = pm_smbchg_bat_if_set_low_batt_volt_threshold(device_index, pm_dbc_bootup_volt_threshold);
      //boot_log_message("Configure Vlowbatt threshold");
   }

   return err_flag; 
}


pm_err_flag_type pm_sbl_config_fg_sram(uint32 device_index)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  FgSramAddrDataEx_type *sram_data_ptr = NULL;
  FgSramAddrDataEx_type pm_sbl_sram_data[SBL_PACKED_SRAM_CONFIG_SIZE];
  pm_model_type pmic_model = PMIC_IS_INVALID;
  boolean sram_enable_config_flag = FALSE;
  int i = 0;

  //Check if any SRAM configuration is needed
  sram_data_ptr = (FgSramAddrDataEx_type*)pm_target_information_get_specific_info(PM_PROP_FG_SPECIFIC_DATA);
  CORE_VERIFY_PTR(sram_data_ptr);
  for (i=0; i< SBL_SRAM_CONFIG_SIZE; i++) 
  {
     sram_enable_config_flag |= sram_data_ptr[i].EnableConfig;
  }
  
  
  if (sram_enable_config_flag == TRUE )
  {
     pmic_model = pm_get_pmic_model(device_index);   //Check if PMIC exists
     if ( (pmic_model != PMIC_IS_INVALID) || (pmic_model != PMIC_IS_UNKNOWN) )
     {
        //boot_log_message("BEGIN: Configure FG SRAM");

        //Pre-process JEITA data
        pm_sbl_sram_data[0].SramAddr = sram_data_ptr[0].SramAddr;
        pm_sbl_sram_data[0].SramData = (sram_data_ptr[3].SramData  << 24)| 
                                       (sram_data_ptr[2].SramData  << 16)|   
                                       (sram_data_ptr[1].SramData  <<  8)|   
                                        sram_data_ptr[0].SramData;
        pm_sbl_sram_data[0].DataOffset = sram_data_ptr[0].DataOffset;  
        pm_sbl_sram_data[0].DataSize = 4;
        //Set JEITA threshould configuration flag
        pm_sbl_sram_data[0].EnableConfig = sram_data_ptr[0].EnableConfig;   
        pm_sbl_sram_data[0].EnableConfig |= sram_data_ptr[1].EnableConfig;
        pm_sbl_sram_data[0].EnableConfig |= sram_data_ptr[2].EnableConfig;
        pm_sbl_sram_data[0].EnableConfig |= sram_data_ptr[3].EnableConfig;

        //Pre-process Thermistor Beta Data
        //thremistor_c1_coeff
        pm_sbl_sram_data[1]  = sram_data_ptr[4];

        //thremistor_c2_coeff and thremistor_c3_coeff
        pm_sbl_sram_data[2].SramAddr   = sram_data_ptr[5].SramAddr;
        pm_sbl_sram_data[2].SramData   = (sram_data_ptr[6].SramData << 16) | sram_data_ptr[5].SramData;
        pm_sbl_sram_data[2].DataOffset = sram_data_ptr[5].DataOffset;  
        pm_sbl_sram_data[2].DataSize = 4;
        pm_sbl_sram_data[2].EnableConfig   = sram_data_ptr[5].EnableConfig;

        //Configure SRAM Data
        err_flag |= PmicFgSram_ProgBurstAccessEx(device_index, pm_sbl_sram_data, SBL_PACKED_SRAM_CONFIG_SIZE);

        //Test: Read Back
        //err_flag |= PmicFgSram_Dump(device_index, 0x0454, 0x0454);
        //err_flag |= PmicFgSram_Dump(device_index, 0x0444, 0x0448);
        //err_flag |= PmicFgSram_Dump(device_index, 0x0448, 0x0452);
        
        //boot_log_message("END: Configure FG SRAM");
     }
  }
  
  return err_flag; 
}
// TINNO BEGIN
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#ifdef FEATURE_CHECK_TEMPERATURE_BOOTUP
#define BATT_TEMP_CHECK_DELAY	(1000*1000*5)	//5 seconds
#define BATT_TEMP_READ_DELAY	(1000*10)	//10ms
pm_err_flag_type pm_sbl_get_batt_temp(uint32 device_index, int *temp)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  uint32 raw;
  uint32 err_count = 0;
  float temp_raw;
	
  while (err_count < 3) {
  		err_flag = PmicFgSram_get_batt_temp_raw(device_index, 0x0552, 0x0554, &raw);
		if (err_flag == PM_ERR_FLAG__SUCCESS) {
			break;
		}

		err_count++;
		snprintf(s_buf, LOG_BUF_SIZE, "%s, raw = %d, err_count = %d", 
  			__FUNCTION__, raw, err_count);
   		boot_log_message(s_buf); 

		if (err_count >= 3) {
  			return PM_ERR_FLAG__INVALID_PARAMETER;
		}

        CHG_VERIFY(pm_clk_busy_wait(BATT_TEMP_READ_DELAY));
  }

  //2bytes in front is battery temperature data 
  raw = (raw >> 16);

  //calcution Degree Kelvin and convert to Degree Celsius
  temp_raw = (raw - 0x1112) * 0.0625;

  *temp = (int)temp_raw;

  return PM_ERR_FLAG__SUCCESS;
}

pm_err_flag_type pm_sbl_batt_temp_check(uint32 device_index)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  int temp = 0;
  boolean charger_present = FALSE;

  err_flag |= pm_sbl_get_batt_temp(device_index, &temp);
  snprintf(s_buf, LOG_BUF_SIZE, "%s, battery temperature = %d, err_flag = %d", 
  	__FUNCTION__, temp, err_flag);
  boot_log_message(s_buf); 

  //check the battery temperature whether in the range of
  //CLOD_TEMP_THRESLOD to HOT_TEMP_THRESLOD
  //poweroff if not charger
  //every BATT_TEMP_CHECK_DELAY check it if have charger
  while (err_flag != PM_ERR_FLAG__SUCCESS || temp <= CLOD_TEMP_THRESLOD || temp >= HOT_TEMP_THRESLOD) {
      CHG_VERIFY(pm_smbchg_chager_active(device_index, &charger_present));
	  if (charger_present == FALSE) {
  			boot_log_message("pm_sbl_batt_temp_check: out range of temperature threshold, no charger poweroff"); 
			boot_hw_powerdown();	//powerdown system
	  }
      CHG_VERIFY(pm_clk_busy_wait(BATT_TEMP_CHECK_DELAY));
	  
  	  err_flag |= pm_sbl_get_batt_temp(device_index, &temp);
	  snprintf(s_buf, LOG_BUF_SIZE, "%s, battery temperature = %d, err_flag = %d", 
  		__FUNCTION__, temp, err_flag);
	  boot_log_message(s_buf); 
  }

  return err_flag;
}
#endif
//TINNO END



pm_err_flag_type pm_sbl_chg_no_battery_chgr_detection(uint32 device_index, boolean dbc_usb_500_mode ) 
{
   pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
   pm_smbchg_misc_src_detect_type chgr_src_detected;
   uint32 usbin_current_limit     = 1500; //Can not be set to lower than 700mA

   if (dbc_usb_500_mode == TRUE)  //500mA limit is desired by OEM
   {
      err_flag |= pm_smbchg_usb_chgpth_en_hvdcp(device_index, FALSE); //Disable HVDCP
      err_flag |= pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__USBIN_MODE_CHG , FALSE); //Clear HC mode bit 
      err_flag |= pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__USB51_MODE, TRUE);    //set USB500 mode
      err_flag |= pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__ICL_OVERRIDE, TRUE);  //Set ICL_OVERRIDE 
   }
   else 
   {
      err_flag |= pm_smbchg_usb_chgpth_en_hvdcp(device_index, FALSE);                                              //Disable HVDCP
      err_flag |= pm_smbchg_usb_chgpth_config_aicl(device_index, PM_SMBCHG_USB_CHGPTH_AICL_CFG__AICL_EN, FALSE);   //Disable AICL 
      err_flag |= pm_smbchg_usb_chgpth_set_usbin_current_limit(device_index, usbin_current_limit);
      err_flag |= pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__USBIN_MODE_CHG, TRUE); //set HC mode
      err_flag |= pm_smbchg_usb_chgpth_set_cmd_il(device_index, PM_SMBCHG_USBCHGPTH_CMD_IL__ICL_OVERRIDE, TRUE);   //Set ICL_OVERRIDE 
   }

   //Detect Charger Type
   err_flag |= pm_smbchg_misc_chgr_port_detected(device_index, &chgr_src_detected);

   boot_log_message(" NO Battery Detected ");

   switch(chgr_src_detected) //Log charger type
   {
       case PM_SMBCHG_MISC_SRC_DETECT_CDP:
            boot_log_message("Charger source: CDP");
            break;
       case PM_SMBCHG_MISC_SRC_DETECT_DCP:
            boot_log_message("Charger source: DCP");
            break;
       case PM_SMBCHG_MISC_SRC_DETECT_SDP:
            boot_log_message("Charger source: SDP");
            break;
       case PM_SMBCHG_MISC_SRC_DETECT_OTHER_CHARGING_PORT:
            boot_log_message("Charger source: OTHER_CHARGING_PORT");
            break;
       default:
            boot_log_message("Charger source: Unknown");
            break;
    }

   return err_flag; 
 }

//add by alik
extern pm_err_flag_type pm_smbchg_bat_if_chg_led(uint32 device_index, boolean enable);
extern boolean tinno_pm_smbchg_usb_chgpth_sts(uint32 device_index);
//add end

pm_err_flag_type pm_sbl_config_chg_parameters(uint32 device_index)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
    static pm_smbchg_specific_data_type *chg_param_ptr;
    boolean batt_ov = FALSE;
    boolean charger_enable_ori = FALSE;
    pm_smbchg_chgr_status_type chg_status;
//add by alik
	if(tinno_pm_smbchg_usb_chgpth_sts(device_index))
	{
           pm_smbchg_bat_if_chg_led(device_index,TRUE);
	}
//add end
   
    if(chg_param_ptr == NULL)
    {
        chg_param_ptr = (pm_smbchg_specific_data_type*)pm_target_information_get_specific_info(PM_PROP_SMBCHG_SPECIFIC_DATA);
        CORE_VERIFY_PTR(chg_param_ptr);
    }

    //Vlowbatt Threshold  
    //  - Done on:  pm_sbl_chg_config_vbat_low_threshold()

    //Charger Path Input Priority 
    if (chg_param_ptr->chgpth_input_priority.enable_config == PM_ENABLE_CONFIG)
    {
        pm_smbchg_chgpth_input_priority_type chgpth_priority = chg_param_ptr->chgpth_input_priority.chgpth_input_priority;
        if (chgpth_priority < PM_SMBCHG_USBCHGPTH_INPUT_PRIORITY_INVALID) 
        {
            err_flag |= pm_smbchg_chgpth_set_input_priority(device_index, chgpth_priority);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //Battery Missing Detection Source 
    if (chg_param_ptr->bat_miss_detect_src.enable_config == PM_ENABLE_CONFIG)
    {
        pm_smbchg_bat_miss_detect_src_type batt_missing_det_src = chg_param_ptr->bat_miss_detect_src.bat_missing_detection_src;
        if (batt_missing_det_src < PM_SMBCHG_BAT_IF_BAT_MISS_DETECT_SRC_INVALID) 
        {
            err_flag |= pm_smbchg_bat_if_set_bat_missing_detection_src(device_index, batt_missing_det_src);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //WDOG Timeout      
    if (chg_param_ptr->wdog_timeout.enable_config == PM_ENABLE_CONFIG)
    {
        pm_smbchg_wdog_timeout_type wdog_timeout = chg_param_ptr->wdog_timeout.wdog_timeout;
        if (wdog_timeout < PM_SMBCHG_MISC_WD_TMOUT_INVALID) 
        {
            err_flag |= pm_smbchg_misc_set_wdog_timeout(device_index, wdog_timeout);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //Enable WDOG                      
    if (chg_param_ptr->enable_wdog.enable_config == PM_ENABLE_CONFIG)
    {
        boolean enable_smbchg_wdog = chg_param_ptr->enable_wdog.enable_wdog;
        err_flag |= pm_smbchg_misc_enable_wdog(device_index, enable_smbchg_wdog);
    }

    //FAST Charging Current            
    if (chg_param_ptr->fast_chg_i.enable_config == PM_ENABLE_CONFIG)
    {
        uint32 fast_chg_i_ma = chg_param_ptr->fast_chg_i.fast_chg_i_ma;
        if ((fast_chg_i_ma >= 300) && (fast_chg_i_ma <= 3000) )
        {
            err_flag |= pm_smbchg_chgr_set_fast_chg_i(device_index, fast_chg_i_ma);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //Pre Charge Current               
    if (chg_param_ptr->pre_chg_i.enable_config == PM_ENABLE_CONFIG)
    {
        uint32 pre_chg_i_ma = chg_param_ptr->pre_chg_i.pre_chg_i_ma;
        if ((pre_chg_i_ma >= 100) && (pre_chg_i_ma <= 550) )
        {
            err_flag |= pm_smbchg_chgr_set_pre_chg_i(device_index, pre_chg_i_ma);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //Pre to Fast Charge Current       
    if (chg_param_ptr->pre_to_fast_chg_theshold_mv.enable_config == PM_ENABLE_CONFIG)
    {
        uint32 p2f_chg_mv = chg_param_ptr->pre_to_fast_chg_theshold_mv.pre_to_fast_chg_theshold_mv;
        if ((p2f_chg_mv >= 2400) && (p2f_chg_mv <= 3000)  )
        {
            err_flag |= pm_smbchg_chgr_set_p2f_threshold(device_index, p2f_chg_mv);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //Float Voltage : 3600mV to 4500 mv                   
    if (chg_param_ptr->float_volt_theshold_mv.enable_config == PM_ENABLE_CONFIG)
    {
        uint32 float_volt_mv = chg_param_ptr->float_volt_theshold_mv.float_volt_theshold_mv;
        if ((float_volt_mv >= 3600) && (float_volt_mv <= 4500))
        {
            // CR827234: If get VBAT_OV, clear CHG_EN, set float voltage and then re-set CHG_EN
            CHG_VERIFY(pm_smbchg_bat_if_irq_status(device_index, PM_SMBCHG_BAT_IF_BAT_OV, PM_IRQ_STATUS_RT, &batt_ov));
            if(batt_ov)
            {
                CHG_VERIFY(pm_smbchg_chgr_get_chgr_sts(device_index, &chg_status));
                charger_enable_ori = chg_status.charger_enable;
                if(chg_status.charger_enable)
                {
                    chg_status.charger_enable = FALSE;
                    CHG_VERIFY(pm_smbchg_bat_if_config_chg_cmd(device_index, PM_SMBCHG_BAT_IF_CMD__EN_BAT_CHG, FALSE));
                }
            }
            err_flag |= pm_smbchg_chgr_set_float_volt(device_index, float_volt_mv);
            if(batt_ov && charger_enable_ori)
            {
                chg_status.charger_enable = TRUE;
                CHG_VERIFY(pm_smbchg_bat_if_config_chg_cmd(device_index, PM_SMBCHG_BAT_IF_CMD__EN_BAT_CHG, TRUE));
            }
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

    //USBIN Input Current Limit  :Valid value is 300 to 3000mAmp      
    if (chg_param_ptr->usbin_input_current_limit.enable_config == PM_ENABLE_CONFIG)
    {
        uint32 usbin_i_limit_ma = chg_param_ptr->usbin_input_current_limit.usbin_input_current_limit;
        if ((usbin_i_limit_ma >= 300) && (usbin_i_limit_ma <= 3000))
        {
            err_flag |= pm_smbchg_usb_chgpth_set_usbin_current_limit(device_index, usbin_i_limit_ma);
        }
        else
        {
            err_flag |= PM_ERR_FLAG__INVALID_PARAMETER;
        }
    }

   return err_flag; 
}

