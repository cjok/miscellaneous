/*! \file
*  
*  \brief  pm_malloc.c ----This file contains the implementation of pm_malloc()
*  \details This file contains the implementation of pm_malloc()
*  
*  &copy; Copyright 2012 Qualcomm Technologies Incorporated, All Rights Reserved
*/

/*===========================================================================

EDIT HISTORY FOR MODULE

This document is created by a code generator, therefore this section will
not contain comments describing changes made to the module.

$Header: //components/rel/boot.bf/3.1.2.c2/boot_images/core/systemdrivers/pmic/framework/src/pm_utils.c#1 $ 

when       who     what, where, why
--------   ---     ----------------------------------------------------------
03/20/14   aab     Added pm_boot_adc_get_mv() 
06/11/13   hs      Adding settling time for regulators.
06/20/12   hs      Created

===========================================================================*/

/*===========================================================================

INCLUDE FILES 

===========================================================================*/
#include "pm_utils.h"
#include "DALSys.h"
#include "CoreVerify.h"
#include "busywait.h"

#ifdef FEATURE_BOOT_BATT_TEMP_CHECK
#include "AdcInputs.h"
#include "boot_logger.h"
#include "boot_logger_timer.h"
#include "smem.h"
#include "pm_chg_common.h"
#include "boothw_target.h"
#include "boot_comdef.h"

#define LOG_BUF_SIZE 128
static char s_buf[LOG_BUF_SIZE] = {0};
#endif


void pm_malloc(uint32 dwSize, void **ppMem)
{
    DALResult dalResult = DAL_SUCCESS;

    dalResult = DALSYS_Malloc(dwSize, ppMem);
    CORE_VERIFY(dalResult == DAL_SUCCESS );
    CORE_VERIFY_PTR(*ppMem);

    DALSYS_memset(*ppMem, 0, dwSize);
}

uint64 pm_convert_time_to_timetick(uint64 time_us)
{
    return (time_us*19200000)/1000000;
}

uint64 pm_convert_timetick_to_time(uint64 time_tick)
{

    return (time_tick*1000000)/19200000;
}



pm_err_flag_type
pm_boot_adc_get_mv(const char *pszInputName, uint32 *battery_voltage)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  AdcBootDeviceChannelType channel;
  AdcBootResultType adc_result;
  uint32 error = 0x0;
  DALResult adc_err;

  /*Initialize the ADC*/
  error = AdcBoot_Init();

  if (0 != error)
  {
    err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
  }
  else
  {
    /*Get the channel from where the data is needed, Like ADC_INPUT_VBATT, ADC_INPUT_XO_THERM */
    adc_err = AdcBoot_GetChannel(pszInputName, &channel);
    if (DAL_SUCCESS != adc_err)
    {
      err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
    }
    else
    {
      /*Read the Voltage of the Battery*/
      adc_err = AdcBoot_Read(&channel, &adc_result);
      if (DAL_SUCCESS != error)
      {
        err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
      }
      /*Check for the result*/
      if (0 != adc_result.eStatus)
      {
        *battery_voltage = (uint32)(adc_result.nMicrovolts / 1000);
      }
      else
      {
        err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
      }
    }
  }
  return err_flag;
}


pm_err_flag_type
pm_clk_busy_wait ( uint32 uS )
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;

  if ( uS > 0 )
  {  
     (void) DALSYS_BusyWait(uS);
  }
  else
  {
     err_flag = PM_ERR_FLAG__PAR1_OUT_OF_RANGE;
  }

  return err_flag;
}

// TINNO BEGIN
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#ifdef FEATURE_BOOT_BATT_TEMP_CHECK
#define BATT_TEMP_CHECK_DELAY	(1000*1000*5)	//5 seconds
#define BATT_TEMP_READ_DELAY	(1000*10)	//10ms
//use adc channel[ADC_INPUT_BATT_THERM] to get battery ntc temperature value
pm_err_flag_type
pm_boot_adc_get_batt_temp(const char *pszInputName, int32 *batt_temp)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  AdcBootDeviceChannelType channel;
  AdcBootResultType adc_result;
  uint32 error = 0x0;
  DALResult adc_err;

  /*Initialize the ADC*/
  error = AdcBoot_Init();

  if (0 != error)
  {
    err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
  }
  else
  {
    /*Get the channel from where the data is needed, Like ADC_INPUT_VBATT, ADC_INPUT_XO_THERM */
    adc_err = AdcBoot_GetChannel(pszInputName, &channel);
    if (DAL_SUCCESS != adc_err)
    {
      err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
    }
    else
    {
      /*Read the Voltage of the Battery*/
      adc_err = AdcBoot_Read(&channel, &adc_result);
      if (DAL_SUCCESS != error)
      {
        err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
      }
      /*Check for the result*/
      if (0 != adc_result.eStatus)
      {
        *batt_temp = (int32)(adc_result.nPhysical);
      }
      else
      {
        err_flag |= PM_ERR_FLAG__ADC_NOT_READY;
      }
    }
  }
  return err_flag;
}

pm_err_flag_type pm_sbl_get_batt_temp(int32 *temp)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  uint32 err_count = 0;
  int32 temp_raw;
	
  while (err_count < 3) {
		err_flag |= pm_boot_adc_get_batt_temp(ADC_INPUT_BATT_THERM, &temp_raw);
		if (err_flag == PM_ERR_FLAG__SUCCESS) {
			break;
		}

		err_count++;
		snprintf(s_buf, LOG_BUF_SIZE, "%s, temp_raw = %d, err_count = %d", 
			__FUNCTION__, temp_raw, err_count);
   		boot_log_message(s_buf); 

		if (err_count >= 3) {
  			return PM_ERR_FLAG__ABUS_IS_BUSY_MODE;
		}

        pm_clk_busy_wait(BATT_TEMP_READ_DELAY);
  }

  *temp = (int32)temp_raw;

  snprintf(s_buf, LOG_BUF_SIZE, "%s, temp_raw = %d, err_count = %d",
  	__FUNCTION__, temp_raw, err_count);
  boot_log_message(s_buf); 

  return PM_ERR_FLAG__SUCCESS;
}

#if 0
static pm_err_flag_type pm_misc_data_store_smem(int32 data)
{
	int32 *misc_smem_ptr = NULL;

    /* Storing the misc data to shared memory */
	misc_smem_ptr = (int32 *) smem_alloc(SMEM_ID_VENDOR0, sizeof(int32));
	if (misc_smem_ptr == NULL){
		return PM_ERR_FLAG__INVALID;
	}

	*misc_smem_ptr = data;

	return PM_ERR_FLAG__SUCCESS;
}
#endif

pm_err_flag_type pm_sbl_batt_temp_check(void)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  int32 temp = 0;
  boolean charger_present = FALSE;

  err_flag |= pm_sbl_get_batt_temp(&temp);

//  err_flag |= pm_misc_data_store_smem(temp);

  snprintf(s_buf, LOG_BUF_SIZE, "%s, battery temperature = %d, err_flag = %d", 
  	__FUNCTION__, temp, err_flag);
  boot_log_message(s_buf); 

  //check the battery temperature whether in the range of
  //CLOD_TEMP_THRESLOD to HOT_TEMP_THRESLOD
  //poweroff if not charger
  //every BATT_TEMP_CHECK_DELAY check it if have charger
  while (err_flag != PM_ERR_FLAG__SUCCESS || temp <= CLOD_TEMP_THRESLOD || temp >= HOT_TEMP_THRESLOD) {
	  pm_chg_is_charger_present(&charger_present); 
	  if (charger_present == FALSE) {
  			boot_log_message("pm_sbl_batt_temp_check: out range of \
				temperature threshold, no charger poweroff"); 
			boot_hw_powerdown();	//powerdown system
	  }
      pm_clk_busy_wait(BATT_TEMP_CHECK_DELAY);
	  
  	  err_flag |= pm_sbl_get_batt_temp(&temp);
	  snprintf(s_buf, LOG_BUF_SIZE, "%s, battery temperature = %d, err_flag = %d", 
  		__FUNCTION__, temp, err_flag);
	  boot_log_message(s_buf); 
  }

  return err_flag;
}
#endif
//TINNO END
