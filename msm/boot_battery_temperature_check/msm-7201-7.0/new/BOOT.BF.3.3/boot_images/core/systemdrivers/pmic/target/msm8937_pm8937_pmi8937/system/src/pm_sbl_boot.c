/*! \file pm_sbl_boot.c
*  \n
*  \brief This file contains PMIC device initialization function where initial PMIC
*  \n SBL settings are configured through the PDM auto-generated code.
*  \n
*  \n &copy; Copyright 2010-2013 QUALCOMM Technologies Incorporated, All Rights Reserved
*/
/* =======================================================================
Edit History
This section contains comments describing changes made to this file.
Notice that changes are listed in reverse chronological order.

$Header: //components/rel/boot.bf/3.3/boot_images/core/systemdrivers/pmic/target/msm8937_pm8937_pmi8937/system/src/pm_sbl_boot.c#3 $

when       who     what, where, why
--------   ---     ----------------------------------------------------------
06/09/15   sv      Added pm_target_ps_hold_cfg API
09/29/14   mr      Updated/Modified PBS Driver and Write PBS ROM Version in
                   REV_ID.PBS_OTP_ID Register (CR-728234)
08/22/14   akt     Added for virtio testing (CR-713705)
01/24/14   rk      remove pm_validate_pon as it is used for SMBC
12/19/13   rk      Added spmi channel config setings
11/22/13   rk      Added optimization of SBL and PBS generated code
07/15/13   aab     Enabled SBL charging
02/26/13   kt      Added PMIC SMEM Init function
02/14/13   kt      Added PBS Info reads and stored the info in Glb Ctxt
01/15/13   aab     Fixed KW error/Clean
12/06/12   hw      replace comdef.h with com_dtypes.h
07/26/12   umr     Add Chip Info Chip ID and Rev.
04/09/12   umr     Add PON reason API for SBL to return pon data.
10/03/11   hs      Added code check to ensure the power rail does get turned
                   on/off before returning.
09/28/11   dy      Replace delay loop with DALSYS_BusyWait
08/14/11   dy      Check for more PMIC revisions
11/09/10   wra     File created for 8960

========================================================================== */

/*===========================================================================

                     INCLUDE FILES

===========================================================================*/

#include "com_dtypes.h"
#include "pm_sbl_boot.h"
#include "pm_config_sbl.h"
#include "pm_pbs_info.h"
#include "device_info.h"
#include "pm_target_information.h"
#include "DALSys.h" /* For DALSYS_BusyWait */
#include "DDIPlatformInfo.h"
#include "DDIChipInfo.h"
#include "pm_comm.h"
#include "SpmiCfg.h"
#include "pm_app_smbchg.h"
#include "pm_pon.h"
#include "CoreVerify.h"
#include "pm_smbchg_driver.h"
#include "pm_smbchg_chgr.h"

/*===========================================================================

                        TYPE DEFINITIONS

===========================================================================*/

/*===========================================================================

                        FUNCTION DEFINITIONS

===========================================================================*/

static pm_err_flag_type
pm_sbl_pre_config(void)
{
  pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
  pm_sbl_specific_data_type *sbl_param_ptr = NULL;

  sbl_param_ptr = (pm_sbl_specific_data_type*)pm_target_information_get_specific_info(PM_PROP_SBL_SPECIFIC_DATA);
  CORE_VERIFY_PTR(sbl_param_ptr);

  err_flag |= pm_log_pon_reasons(sbl_param_ptr->verbose_uart_logging);

  return err_flag;
}


static pm_err_flag_type
pm_device_setup(void)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;

    Spmi_Result spmi_err_flag = SpmiCfg_Init(TRUE);
    if ( spmi_err_flag != SPMI_SUCCESS ) 
    {
        err_flag = PM_ERR_FLAG__SPMI_OPT_ERR;
    }

    /* PMIC peripheral to SPMI Channel mapping.
    This function must be called before calling any SPMI R/W. */
    err_flag |= pm_target_information_spmi_dyn_chnl_cfg(); 

    err_flag |= pm_comm_channel_init_internal();

    err_flag |= pm_version_detect();

    pm_target_information_init();
    
    pm_comm_info_init();

    return err_flag;
}

pm_err_flag_type
pm_sbl_chg_init (void)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;
    uint8 device_index = 0xFF;
	
    device_index = pm_smbchg_get_index();
    
    if(0xFF == device_index)
    {
        return PM_ERR_FLAG__CHARGER_MODULE_ABSENT;
    }
	
    err_flag |= pm_sbl_chg_pre_init();

    err_flag |= pm_sbl_config_chg_parameters(device_index);

    //Configure FG parameters to SRAM
    err_flag |= pm_sbl_config_fg_sram(device_index);  //Needs to be called before DBC check

	// TINNO BEGIN                                                                               
	// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#ifdef FEATURE_CHECK_TEMPERATURE_BOOTUP
	err_flag |= pm_sbl_batt_temp_check(device_index);
#endif
	// TINNO END

    if(PMIC_IS_PMI8950 == pm_get_pmic_model(device_index))
    {
        err_flag |= pm_sbl_chg_check_weak_battery_status(device_index); 
    }

    err_flag |= pm_sbl_chg_post_init();  

    return err_flag;
}

pm_err_flag_type pm_device_init ( void )
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;

    err_flag |= pm_device_setup();

    err_flag |= pm_device_pre_init();

    err_flag |= pm_pon_init();

    err_flag |= pm_sbl_pre_config(); /* SBL Pre Configuration */

    err_flag |= pm_sbl_config(); /* SBL Configuration */

    err_flag |= pm_pbs_info_init();  /* Read PBS INFO for the pmic devices */

    err_flag |= pm_device_post_init(); /* Initialize PMIC with the ones PDM can not perform */

    return err_flag; /* NON ZERO return means an ERROR */
}

pm_err_flag_type 
pm_target_ps_hold_cfg(pmapp_ps_hold_cfg_type ps_hold_cfg)
{
    pm_err_flag_type err_flag = PM_ERR_FLAG__SUCCESS;

    switch(ps_hold_cfg)
    {
       case PMAPP_PS_HOLD_CFG_WARM_RESET:
        err_flag = pm_pon_ps_hold_cfg(0, PM_PON_RESET_CFG_WARM_RESET);
        err_flag += pm_pon_ps_hold_cfg(1, PM_PON_RESET_CFG_WARM_RESET);        
          break;
       case PMAPP_PS_HOLD_CFG_HARD_RESET:
        err_flag = pm_pon_ps_hold_cfg(0, PM_PON_RESET_CFG_HARD_RESET);
        err_flag += pm_pon_ps_hold_cfg(1, PM_PON_RESET_CFG_NORMAL_SHUTDOWN);
          break;
       case PMAPP_PS_HOLD_CFG_NORMAL_SHUTDOWN:
        err_flag = pm_pon_ps_hold_cfg(0, PM_PON_RESET_CFG_NORMAL_SHUTDOWN);
        err_flag += pm_pon_ps_hold_cfg(1, PM_PON_RESET_CFG_NORMAL_SHUTDOWN);

          break;

       default:
          return PM_ERR_FLAG__FEATURE_NOT_SUPPORTED;
    }

    return err_flag;
}
