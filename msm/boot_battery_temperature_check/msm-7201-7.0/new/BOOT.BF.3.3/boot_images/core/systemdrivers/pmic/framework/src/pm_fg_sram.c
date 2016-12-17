 /*! @file PmicFgSram.c
 *
 * PMIC Battery Profile SRAM/OTP access functionalities 
 * FG OTP Starts at address 0x0.
 * 256x24 (3 banks of 256x8) byte addressable within a 32 bit word.
 * MSByte of each 32 bit word is invalid and will readback 0x00 and will not be programmed.
 * 
 * FG RAM Starts at address 0x400.
 * 128x32 byte addressable.
 *
 * Copyright (c) 2014 Qualcomm Technologies, Inc.  All Rights Reserved. 
 * Qualcomm Technologies Proprietary and Confidential.
 */

/*=============================================================================
                              EDIT HISTORY

  $Header: //components/rel/boot.bf/3.3/boot_images/core/systemdrivers/pmic/framework/src/pm_fg_sram.c#1 $
  $DateTime: 2015/07/02 04:11:47 $
  $Author: pwbldsvc $

 when          who     what, where, why
 --------   ---     -----------------------------------------------------------
 09/22/14   aab     Updated to support FG SRAM configuration in SBL
 05/19/14   va      New file.
=============================================================================*/

/**
  PMIC Platform interface
 */


/**
  FG interface
 */
#include "pm_fg_sram.h"
#include "pm_fg_memif.h"
#include "boot_logger.h"
#include "pm_utils.h"
#include "CoreVerify.h"
#include "smem.h"
#include "pm_target_information.h"

// TINNO BEGIN  
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#include "pm_app_smbchg.h"
// TINNO END


/*===========================================================================
                               MACROS
===========================================================================*/
#define FG_SRAM_START_ADDRESS 0x400
#define FG_SRAM_END_ADDRESS   0x5FF
#define FG_SRAM_ADDR_INCREMENT 4
#define FG_MEM_AVAILABLE_RT_STS_POLL_MIN_TIME 150 //in ms
#define FG_MEM_AVAILABLE_RT_STS_POLL_MAX_TIME 1470 // in ms
#define FG_SRAM_PROFILE_START_ADDRESS 0x4C0
#define FG_SRAM_PROFILE_END_ADDRESS   0x53F
#define FG_SRAM_PROFILE_RANGE 0x7F
#define FG_SRAM_PROFILE_CHK_ADDR    0x053C //refer MDOS for address, data values
#define FG_SRAM_PROFILE_CHK_OFFSET  0x0
#define FG_SRAM_PROFILE_CHK_ENABLE  0x01

#define OFFSET_ZERO 0
#define OFFSET_ONE 1
#define OFFSET_TWO 2
#define OFFSET_THREE 3
#define DATASIZE_ONE 1
#define DATASIZE_TWO 2
#define DATASIZE_THREE 3
#define DATASIZE_FOUR 4
#define DATASIZE_ZERO 0


/*=========================================================================
                            GLOBAL VARIABLES
===========================================================================*/
static FgSramState SramState;
/*===========================================================================
                               TYPE DEFINITIONS
===========================================================================*/



/*==========================================================================
                        LOCAL API PROTOTYPES
===========================================================================*/

pm_err_flag_type PmicFgSram_SetState(FgSramState FgSramSt);
pm_err_flag_type PmicFgSram_ReleaseFgSramAccess(uint32  PmicDeviceIndex);
pm_err_flag_type PmicFgSram_RequestFgSramAccess(uint32  PmicDeviceIndex, 
                                                       pm_fg_memif_mem_intf_cfg  mem_intf_cfg,
                                                       boolean Fast_Memory_Access);
//Continuous Poll on Sram memory access 
pm_err_flag_type PmicFgSram_PollFgSramAccess(uint32 PmicDeviceIndex, boolean *SramAccessStatus);
pm_err_flag_type PmicFgSram_ReadData(uint32 PmicDeviceIndex, uint16 ReadAddress, uint32 *fg_memif_data);

pm_err_flag_type PmicFgSram_WriteData(uint32 PmicDeviceIndex, uint16 WriteAddress, uint32 fg_memif_data);

pm_err_flag_type PmicFgSram_WriteOffsetData(uint32 PmicDeviceIndex, uint16 WriteAddress, uint8 Data, uint8 Offset);

void  PmicFgSram_PrintState(FgSramState FgSramSt);
/*==========================================================================
                        GLOBAL API DEFINITION
===========================================================================*/
/**
PmicFgSram_Init()

@brief
Initializes Sram State
*/
pm_err_flag_type PmicFgSram_Init(FgSramState FgSramState)
{
  //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_Init \n\r"));
  //  PmicFgSram_PrintState(FgSramState);
  if (FG_SRAM_STATUS_INIT == FgSramState || FG_SRAM_STATUS_AVAILABLE == FgSramState ){
    SramState = FgSramState;
  }else{
    SramState = FG_SRAM_STATUS_INIT;
  }
  return PM_ERR_FLAG__SUCCESS;
}

/**
PmicFgSram_GetState()

@brief
Returns Current Sram State
*/
pm_err_flag_type PmicFgSram_GetState(FgSramState *FgSramSt)
{
  pm_err_flag_type  Status  = PM_ERR_FLAG__SUCCESS;
  if ( FG_SRAM_STATUS_INVALID == SramState)
  {
    //Status = EFI_DEVICE_ERROR;
    Status = PM_ERR_FLAG__FG_SRAM_DEVICE_ACCESS;
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_SetState: Error SramState = (%d) \n\r", SramState));
  }
  else{
    *FgSramSt = SramState;
  }

  return Status;
}



/**
PmicFgSram_Dump()

@brief
Dump Sram contents for given range
*/
pm_err_flag_type PmicFgSram_Dump(uint32 PmicDeviceIndex, uint32 DumpSramStartAddr, uint32 DumpSramEndAddr)
{
  pm_err_flag_type       Status   = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;
  uint32           ReadData = 0;
  uint16           startSramAddress;
  uint16           endSramAddress;
  uint16           sramAddressIncrement = FG_SRAM_START_ADDRESS;
  boolean          SramAccessStatus     = FALSE;

  //Validate start and end address if they are not correct dump all SRAM 
  if ((DumpSramStartAddr < FG_SRAM_START_ADDRESS) || (DumpSramEndAddr > FG_SRAM_END_ADDRESS)){
    //CHARGER_DEBUG(( EFI_D_ERROR, "# PmicFgSram_Dump: Over Riding given SRAM address DumpSramStartAddr = (0x%x), DumpSramEndAddr = (0x%x)\n\r", DumpSramStartAddr, DumpSramEndAddr));
    startSramAddress = FG_SRAM_START_ADDRESS;
    endSramAddress   = FG_SRAM_END_ADDRESS;
  }
  else{
    //CHARGER_DEBUG(( EFI_D_ERROR, "# PmicFgSram_Dump: SRAM dump Start = (0x%x) End = (0x%x)address \n\r", DumpSramStartAddr, DumpSramEndAddr));
    startSramAddress = DumpSramStartAddr;
    endSramAddress   = DumpSramEndAddr;
    sramAddressIncrement = DumpSramStartAddr;
  }
  //Request the memory access
  PmicFgSram_RequestFgSramAccess(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, FALSE);

  /*Poll for Sram memory access */
  Status = PmicFgSram_PollFgSramAccess(PmicDeviceIndex, &SramAccessStatus);

  if (PM_ERR_FLAG__SUCCESS == Status && TRUE == SramAccessStatus)
  {
    /*Enables RIF memory interface and the RIF Memory Access Mode.  1 */
    err_flg |=  pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, TRUE);
    //Request Burst access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_BURST, TRUE);

    //Requet read access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, FALSE);

    //Start loop to read and print all Sram contents this could be a 900ms operation 

    //Updated Read address only once.. in burst access FG internally increment address 
    err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, startSramAddress);

    do{
      //Read Data
      err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, &ReadData);
      //Print proper address value pair below print would do 4 byte print
      //CHARGER_DEBUG(( EFI_D_ERROR, "# PmicFgSram_Dump err_flg Code: sramAddress = (0x%x) = ReadData = (0x%x) \n\r", sramAddressIncrement , ReadData));
      sramAddressIncrement += FG_SRAM_ADDR_INCREMENT;

      //Write incremented sream register address 
      err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, sramAddressIncrement);

    }while(((sramAddressIncrement + FG_SRAM_ADDR_INCREMENT) <= endSramAddress )&& (PM_ERR_FLAG__SUCCESS == err_flg));
  }
  else{
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_PollFgSramAccess Failed : Status = (%d) SramAccessStatus = (%d) \n\r", Status, SramAccessStatus));
  }

  // After read completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0 ..
  // Release the memory access
  Status |= PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

  Status |= err_flg;
  return Status;
}

// TINNO BEGIN  
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#ifdef FEATURE_CHECK_TEMPERATURE_BOOTUP
#define LOG_BUF_SIZE 128
static char s_buf[LOG_BUF_SIZE] = {0}; 
pm_err_flag_type PmicFgSram_get_batt_temp_raw(uint32 PmicDeviceIndex, uint32 DumpSramStartAddr, uint32 DumpSramEndAddr, uint32 *raw)
{
  pm_err_flag_type       Status   = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;
  uint32           ReadData = 0;
  uint16           startSramAddress;
  uint16           endSramAddress;
  uint16           sramAddressIncrement = FG_SRAM_START_ADDRESS;
  boolean          SramAccessStatus     = FALSE;

  //Validate start and end address if they are not correct dump all SRAM 
  if ((DumpSramStartAddr < FG_SRAM_START_ADDRESS) || (DumpSramEndAddr > FG_SRAM_END_ADDRESS)){
    snprintf(s_buf, LOG_BUF_SIZE, "# PmicFgSram_Dump: Over Riding given SRAM address DumpSramStartAddr = (0x%x), DumpSramEndAddr = (0x%x)", DumpSramStartAddr, DumpSramEndAddr);
	boot_log_message(s_buf);

    startSramAddress = FG_SRAM_START_ADDRESS;
    endSramAddress   = FG_SRAM_END_ADDRESS;
  }
  else{
    snprintf(s_buf, LOG_BUF_SIZE,"# PmicFgSram_Dump: SRAM dump Start = (0x%x) End = (0x%x)address", DumpSramStartAddr, DumpSramEndAddr);
	boot_log_message(s_buf);
    startSramAddress = DumpSramStartAddr;
    endSramAddress   = DumpSramEndAddr;
    sramAddressIncrement = DumpSramStartAddr;
  }
  //Request the memory access
  PmicFgSram_RequestFgSramAccess(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, FALSE);

  /*Poll for Sram memory access */
  Status = PmicFgSram_PollFgSramAccess(PmicDeviceIndex, &SramAccessStatus);

  if (PM_ERR_FLAG__SUCCESS == Status && TRUE == SramAccessStatus)
  {
    /*Enables RIF memory interface and the RIF Memory Access Mode.  1 */
    err_flg |=  pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, TRUE);
    //Request Burst access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_BURST, TRUE);

    //Requet read access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, FALSE);

    //Start loop to read and print all Sram contents this could be a 900ms operation 

    //Updated Read address only once.. in burst access FG internally increment address 
    err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, startSramAddress);

    do{
      //Read Data
      err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, &ReadData);
      //Print proper address value pair below print would do 4 byte print
     snprintf(s_buf, LOG_BUF_SIZE, "# PmicFgSram_Dump err_flg = %d Code: sramAddress = (0x%x) = ReadData = (0x%x)", 
	 	err_flg,sramAddressIncrement , ReadData);
	 boot_log_message(s_buf);
	 *raw = ReadData;
      sramAddressIncrement += FG_SRAM_ADDR_INCREMENT;

      //Write incremented sream register address 
      err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, sramAddressIncrement);

    }while(((sramAddressIncrement + FG_SRAM_ADDR_INCREMENT) <= endSramAddress )&& (PM_ERR_FLAG__SUCCESS == err_flg));
  }
  else{
    snprintf(s_buf, LOG_BUF_SIZE, "## PmicFgSram_PollFgSramAccess Failed : Status = (%d) SramAccessStatus = (%d)", Status, SramAccessStatus);
	 boot_log_message(s_buf);
  }

  // After read completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0 ..
  // Release the memory access
  Status |= PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

  Status |= err_flg;
  return Status;
}
#endif
// TINNO END


/**
PmicFgSram_ProgBurstAccess()

@brief
Write Bytes (uint32) Array in given Address pair
*/
pm_err_flag_type PmicFgSram_ProgBurstAccess(uint32 PmicDeviceIndex, FgSramAddrData * AddrDataPair, uint32 AddrDataCount)
{
  pm_err_flag_type       Status   = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;
  boolean          SramAccessStatus     = FALSE;
  uint32           CurrCount = 0;

  //Validate input params
  if ((!AddrDataPair) || (0 == AddrDataCount) || (AddrDataCount >= FG_SRAM_MAX_SIZE))
    return PM_ERR_FLAG__INVALID_PARAMETER;
  
  //Request the memory access
  Status = PmicFgSram_RequestFgSramAccess(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, TRUE);

  /*Poll for Sram memory access */
  Status |= PmicFgSram_PollFgSramAccess(PmicDeviceIndex, &SramAccessStatus);

  if (PM_ERR_FLAG__SUCCESS == Status && TRUE == SramAccessStatus)
  {

    //Request Burst access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_BURST, TRUE);

    //Requet write access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, TRUE);

    //Write address 
    err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, AddrDataPair[CurrCount].SramAddr);

    //write in loop
    do{
      //Read Data
      err_flg |=  pm_fg_memif_write_data(PmicDeviceIndex, AddrDataPair[CurrCount].SramData);
    }while(++CurrCount < AddrDataCount);

  }else{
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_ProgBurstAccess Failed : Status = (%d) SramAccessStatus = (%d) \n\r", Status, SramAccessStatus));
    err_flg |= Status;
  }

  //After read completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0
  PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

  if(PM_ERR_FLAG__SUCCESS == err_flg)
  {
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgBurstAccess err_flg Code: (%d) AddrDataCount = (0x%x)\n\r", err_flg, AddrDataCount));
  }
  else{
    Status |= err_flg ;
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgBurstAccess err_flg Code: %d \n\r", err_flg));
  }

  return Status;
}




/**
PmicFgSram_ProgBurstAccessEx()

@brief
Write Bytes Based on given dataoffert and size Array in given Address pair
*/
pm_err_flag_type PmicFgSram_ProgBurstAccessEx(uint32 PmicDeviceIndex, FgSramAddrDataEx_type * AddrDataPairEx, uint32 AddrDataCount)
{
  pm_err_flag_type Status   = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;
  boolean          SramAccessStatus     = FALSE;
  uint32           CurrCount = 0;
  uint32           Mask      = 0;
  uint32           ReadData  = 0;
  uint32           WriteData = 0;

  //Validate input params
  if ((!AddrDataPairEx) || (0 == AddrDataCount) || (AddrDataCount >= FG_SRAM_MAX_SIZE))
  {
    return PM_ERR_FLAG__INVALID_PARAMETER;
  }

  //Request the memory access
  Status = PmicFgSram_RequestFgSramAccess(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, TRUE);

  /*Poll for Sram memory access */
  Status |= PmicFgSram_PollFgSramAccess(PmicDeviceIndex, &SramAccessStatus);

  if (PM_ERR_FLAG__SUCCESS == Status && TRUE == SramAccessStatus)
  {
    //write in loop
    do{
      if ((AddrDataPairEx[CurrCount].DataSize   > FG_SRAM_RD_WR_BUS_WIDTH )|| 
          (AddrDataPairEx[CurrCount].DataOffset > FG_SRAM_RD_WR_OFFSET_WIDTH ))
      {
        Status = PM_ERR_FLAG__INVALID_PARAMETER;
        break;
      }
      if (AddrDataPairEx[CurrCount].EnableConfig == 0) 
      {
         continue; //skip configuration
      }
      //Request Burst access 
      err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_BURST, FALSE);

      switch(AddrDataPairEx[CurrCount].DataSize)
      {
        case DATASIZE_FOUR:
        {
          if (AddrDataPairEx[CurrCount].DataOffset != OFFSET_ZERO){
            Status = PM_ERR_FLAG__INVALID_PARAMETER;
            break;
          }
          WriteData  = AddrDataPairEx[CurrCount].SramData;
        }
        break;
        case DATASIZE_THREE:
        {
          if (AddrDataPairEx[CurrCount].DataOffset > OFFSET_ONE){
            Status = PM_ERR_FLAG__INVALID_PARAMETER;
            break;
          }
          Mask = ~(0xFFFFFF << (AddrDataPairEx[CurrCount].DataOffset  * NUM_BITS_IN_BYTE /*8*/));
        }
        break;

        case DATASIZE_TWO:
        {
          if (AddrDataPairEx[CurrCount].DataOffset > OFFSET_TWO){
            Status = PM_ERR_FLAG__INVALID_PARAMETER;
            break;
          }
            Mask = ~(0xFFFF << (AddrDataPairEx[CurrCount].DataOffset  * NUM_BITS_IN_BYTE /*8*/));
        }
        break;

        case DATASIZE_ONE:
        {
          Mask = ~(0xFF << (AddrDataPairEx[CurrCount].DataOffset  * NUM_BITS_IN_BYTE /*8*/));
        }
        break;

        case DATASIZE_ZERO: //Skip current Entry
          Status = PM_ERR_FLAG__SUCCESS;
        continue;

        default:
        break;
      }
      if(PM_ERR_FLAG__SUCCESS == Status)
      {
        if (AddrDataPairEx[CurrCount].DataSize != DATASIZE_FOUR)
        {
          //Read Data from SRAM to avoid overwrite on other bytes 
          //Request Read Access
          err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, FALSE);
          
          //Write address 
          err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, AddrDataPairEx[CurrCount].SramAddr);
          
          //Read Data
          err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, &ReadData);
        
          WriteData = ((ReadData & Mask) | ( AddrDataPairEx[CurrCount].SramData << (AddrDataPairEx[CurrCount].DataOffset * NUM_BITS_IN_BYTE)));
        }
        //Requet write access 
        err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, TRUE);
        
        //Write address 
        err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, AddrDataPairEx[CurrCount].SramAddr);
        //Read Data
        err_flg |=  pm_fg_memif_write_data(PmicDeviceIndex, WriteData);
      }
    }while(++CurrCount < AddrDataCount);
  }
  else{
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_ProgBurstAccess Failed : Status = (%d) SramAccessStatus = (%d) \n\r", Status, SramAccessStatus));
    err_flg |= Status;
  }

  //After read completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0
  PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

  if(PM_ERR_FLAG__SUCCESS == err_flg){
       //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgBurstAccess err_flg Code: (%d) AddrDataCount = (0x%x)\n\r", err_flg, AddrDataCount));
  }
  else{
    Status |= err_flg ;
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgBurstAccess err_flg Code: %d \n\r", err_flg));
  }

  return Status;
}


/**
PmicFgSram_ProgSingleAccess()

@brief
Write Bytes (uint8) Array in given Address pair
*/
pm_err_flag_type PmicFgSram_ProgSingleAccess(uint32 PmicDeviceIndex, FgSramAddrDataOffset * AddrDataPair,uint32 AddrDataCount)
{
  pm_err_flag_type Status    = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg   = PM_ERR_FLAG__SUCCESS;
  boolean          SramAccessStatus     = FALSE;
  uint8            Offset    = 0;
  uint32           CurrCount = 0;

  //Validate input params
  if ((!AddrDataPair) || (0 == AddrDataCount) || (AddrDataCount >= FG_SRAM_MAX_SIZE))
    return PM_ERR_FLAG__INVALID_PARAMETER;
  
  //Request the memory access
  Status = PmicFgSram_RequestFgSramAccess(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, FALSE);

  /*Poll for Sram memory access */
  Status |= PmicFgSram_PollFgSramAccess(PmicDeviceIndex, &SramAccessStatus);

  if (PM_ERR_FLAG__SUCCESS == Status && TRUE == SramAccessStatus)
  {
    //Request single access 
    err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_BURST, FALSE);

    //write in loop
    do{
      //Know and validate the offset otherwise break the loop and return invalid 
      if(3 >= AddrDataPair[CurrCount].DataOffset){
        Offset = AddrDataPair[CurrCount].DataOffset;
      }else{
        //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_ProgSingleAccess InValid Offset: Offset passed = (%d) \n\r", AddrDataPair[CurrCount].DataOffset));
        Status = PM_ERR_FLAG__FG_SRAM_DEVICE_ACCESS;
        err_flg = PM_ERR_FLAG__PAR1_OUT_OF_RANGE;
        break;
      }
      Status |= PmicFgSram_WriteOffsetData(PmicDeviceIndex, AddrDataPair[CurrCount].SramAddr,
                                                 AddrDataPair[CurrCount].SramData, Offset);
    }while(++CurrCount < AddrDataCount && PM_ERR_FLAG__SUCCESS == Status);
  }
  else{
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_ProgSingleAccess Failed : Status = (%d) SramAccessStatus = (%d) \n\r", Status, SramAccessStatus));
    err_flg |= Status;
  }

  //After read completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0
  PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

  if(PM_ERR_FLAG__SUCCESS == err_flg || PM_ERR_FLAG__SUCCESS != Status)
  {
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgSingleAccess err_flg Code: (%d) Status = (%d) AddrDataCount = (0x%x)\n\r", err_flg, Status, AddrDataCount));
  }
  else{
    Status |= err_flg ;
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_ProgSingleAccess err_flg Code: %d \n\r", err_flg));
  }

  return Status;
}


/*==========================================================================
                        LOCAL  API DEFINITION
===========================================================================*/
/**
PmicFgSram_SetState()

@brief
Set Sram State
*/
pm_err_flag_type PmicFgSram_SetState(FgSramState FgSramSt)
{
  pm_err_flag_type  Status  = PM_ERR_FLAG__SUCCESS;
  if ( FG_SRAM_STATUS_INVALID == SramState)
  {
    Status = PM_ERR_FLAG__FG_SRAM_DEVICE_ACCESS;
    //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_SetState: Error SramState = (%d) \n\r", SramState));
  }
  else{
    SramState = FgSramSt;
    //PmicFgSram_PrintState(FgSramSt);
    PmicFgSram_PrintState(SramState);
  }

  return Status;
}

/**
PmicFgSram_PrintState()

@brief
Debug Print Sram State
*/
void PmicFgSram_PrintState(FgSramState FgSramSt)
{
  switch(FgSramSt){
    case FG_SRAM_STATUS_INIT:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_INIT SramState: = (%d) \n\r", SramState));
    break;
    case FG_SRAM_STATUS_LOAD_PROFILE:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_LOAD_PROFILE SramState: = (%d) \n\r", SramState));
    break;
    case FG_SRAM_STATUS_IN_USE:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_IN_USE SramState: = (%d) \n\r", SramState));
    break;
    case FG_SRAM_STATUS_POLLING:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_POLLING SramState: = (%d) \n\r", SramState));
    break;
    case FG_SRAM_STATUS_AVAILABLE:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_AVAILABLE SramState: = (%d) \n\r", SramState));
    break;
    case FG_SRAM_STATUS_INVALID:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_INVALID SramState: = (%d) \n\r", SramState));
    break;
    default:
      //CHARGER_DEBUG(( EFI_D_ERROR, "## FG_SRAM_STATUS_DEFAULT SramState: = (%d) \n\r", SramState));
      break;
  }
  return;
}

/**
PmicFgSram_WriteData()

@brief
Write 4 bytes (uint32) in given Sram address
*/
pm_err_flag_type PmicFgSram_WriteData(uint32 PmicDeviceIndex, uint16 WriteAddress, uint32 fg_memif_data)
{
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;

  //Write  Address 
  err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, WriteAddress);
  
  //Write Data
  err_flg |=  pm_fg_memif_write_data(PmicDeviceIndex, fg_memif_data);

  return err_flg;

}

/**
PmicFgSram_WriteOffsetData()

@brief
Write 1 Offset byte from given Sram Address
*/
pm_err_flag_type PmicFgSram_WriteOffsetData(uint32 PmicDeviceIndex, uint16 WriteAddress, 
                                               uint8 Data, uint8 Offset)
{
  pm_err_flag_type       Status    = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg   = PM_ERR_FLAG__SUCCESS;
  uint32           ReadData  = 0;
  uint32           WriteData = 0;
  uint32           Mask      = 0;

  //Request Read Access
  err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, FALSE);
  
  //Write address 
  err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, WriteAddress);
  
  //Read Data
  err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, &ReadData);
  //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_WriteOffsetData: ReadData = (0x%x) \n\r", ReadData));
  
  //Generate Mask 
  Mask = ~(0xFF << (((NUM_BYTE_IN_WORD - 1) - Offset ) * NUM_BITS_IN_BYTE /*8*/));
  //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_WriteOffsetData: Mask = (0x%x) = (0x%x) Offset = (0x%x)\n\r", Mask, (ReadData & Mask), Offset));

  WriteData = ((ReadData & Mask) | ( Data << (((NUM_BYTE_IN_WORD - 1) - Offset ) * NUM_BITS_IN_BYTE)));
  //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_WriteOffsetData WriteData = (0x%x) \n\r", WriteData));
  
  //Request Write Access
  err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, TRUE);
  
  //Write address 
  err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, WriteAddress);
  
  err_flg |=  pm_fg_memif_write_data(PmicDeviceIndex, WriteData);

   //Debug Read for what we have written is correct 
   //Request Read Access
   err_flg |=  pm_fg_memif_set_mem_intf_ctl(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CTL_WR_EN, FALSE);
   
   //Write address 
   err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, WriteAddress);
  
   //Read Data
   err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, &ReadData);
   //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_WriteOffsetData: ReadData = (0x%x) \n\r", ReadData));

  return (Status |= err_flg);

}


/**
PmicFgSram_ReadData()

@brief
Reads 4 bytes (uint32) in given Sram address
*/
pm_err_flag_type PmicFgSram_ReadData(uint32 PmicDeviceIndex, uint16 ReadAddress, uint32 *fg_memif_data)
{
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;

  //Write address 
  err_flg |=  pm_fg_memif_write_addr(PmicDeviceIndex, ReadAddress);
  
  //Read Data
  err_flg |=  pm_fg_memif_read_data_reg(PmicDeviceIndex, fg_memif_data);

  return err_flg;
}

/**
PmicFgSram_ReleaseFgSramAccess()

@brief
Release Sram access, Clears memory access bit 
*/
pm_err_flag_type PmicFgSram_ReleaseFgSramAccess
(
  uint32  PmicDeviceIndex
)
{
  pm_err_flag_type       Status  = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;

  //Clear Low latency Memory Access 
  err_flg = pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_LOW_LATENCY_ACS_EN, FALSE);

  //After write completion clear access bit request i.e. RIF_MEM_ACCESS_REQ = 0
  err_flg |= pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ, FALSE);
  //Set Sram module internal state 
  Status  = PmicFgSram_SetState(FG_SRAM_STATUS_AVAILABLE);  

  return (Status |= err_flg );
}

/**
PmicFgSram_RequestFgSramAccess()

@brief
Request Sram access, Sets memory access bit 
*/
pm_err_flag_type PmicFgSram_RequestFgSramAccess
(
  uint32  PmicDeviceIndex, pm_fg_memif_mem_intf_cfg  mem_intf_cfg, boolean Fast_Memory_Access
)
{
  pm_err_flag_type       Status   = PM_ERR_FLAG__SUCCESS;
  pm_err_flag_type err_flg  = PM_ERR_FLAG__SUCCESS;

  //1. Requet access to FG i.e. RIF_MEM_ACCESS_REQ = 1

  if (TRUE == Fast_Memory_Access){
    err_flg = pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, PM_FG_MEMIF_MEM_INTF_CFG_LOW_LATENCY_ACS_EN, TRUE);
    //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_RequestFgSramAccess: Requesting LOW_LATENCY_ACS_EN \n\r"));
  }
  /*Enables RIF memory interface and the RIF Memory Access Mode.  1 */
  err_flg |= pm_fg_memif_set_mem_intf_cfg(PmicDeviceIndex, mem_intf_cfg/*PM_FG_MEMIF_MEM_INTF_CFG_RIF_MEM_ACCESS_REQ */, TRUE);

  //Set Sram module internal state 
  Status = PmicFgSram_SetState(FG_SRAM_STATUS_IN_USE);
  

  return (Status |= err_flg);
}

/**
PmicFgSram_PollFgSramAccess()

@brief
Poll Sram memory access untill timeout or returns when memory access is permitted 
*/
pm_err_flag_type PmicFgSram_PollFgSramAccess
(
  uint32  PmicDeviceIndex, boolean * SramAccessStatus
)
{
  pm_err_flag_type  Status  = PM_ERR_FLAG__SUCCESS;
  boolean Mem_Available_status = FALSE;
  pm_err_flag_type err_flg = PM_ERR_FLAG__SUCCESS;
  uint16 Ttl_spent_time_in_polling = 0;

  *SramAccessStatus = FALSE;

  //Set Sram module internal state 
  PmicFgSram_SetState(FG_SRAM_STATUS_POLLING);

  //Poll FG_MEM_AVAIL_RT_STS = 1 
  do{
      // mem available best time is 150 so first time this call is supposed to fail and successive call may return mem available status as TRUE
      err_flg |=  pm_fg_memif_irq_status(PmicDeviceIndex, PM_FG_MEMIF_FG_MEM_AVAIL_RT_STS, PM_IRQ_STATUS_RT, &Mem_Available_status);
      //CHARGER_DEBUG(( EFI_D_ERROR, "PmicFgSram_PollFgSramAccess PM_FG_MEMIF_FG_MEM_AVAIL_RT_STS : = (%d) \n\r", Mem_Available_status));

      if ( TRUE == Mem_Available_status )
      {
        *SramAccessStatus = TRUE;
        break;
      }//check for error condition as we do not want to loop forever
      else if (Ttl_spent_time_in_polling >= FG_MEM_AVAILABLE_RT_STS_POLL_MAX_TIME)
      {
        *SramAccessStatus = FALSE;
        Status = PM_ERR_FLAG__FG_SRAM_DEVICE_ACCESS;
        //CHARGER_DEBUG(( EFI_D_ERROR, "## PmicFgSram_PollFgSramAccess TimeOut : Ttl_spent_time_in_polling = (%d) \n\r", Ttl_spent_time_in_polling));
        break;
      }

      /*wait for 150 ms before querying mem available status again */
      //gBS->Stall(FG_MEM_AVAILABLE_RT_STS_POLL_MIN_TIME * 1000);
      pm_clk_busy_wait(FG_MEM_AVAILABLE_RT_STS_POLL_MIN_TIME * 1000);

      Ttl_spent_time_in_polling += FG_MEM_AVAILABLE_RT_STS_POLL_MIN_TIME;
  }while(TRUE);

  //Set Sram module internal state 
  if (FALSE == *SramAccessStatus){
    //Clear memory access bit request i.e. RIF_MEM_ACCESS_REQ = 0
    PmicFgSram_ReleaseFgSramAccess(PmicDeviceIndex);

    //might need to read cycle streach bit and clear it here
    PmicFgSram_SetState(FG_SRAM_STATUS_AVAILABLE); //when time out for polling access request 
  }
  else 
    PmicFgSram_SetState(FG_SRAM_STATUS_IN_USE);

  return (Status |= err_flg);
}



