#ifndef __PMICFGSRAM_H__
#define __PMICFGSRAM_H__

 /*! @file PmicFgSram.h
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

  $Header: //components/rel/boot.bf/3.3/boot_images/core/systemdrivers/pmic/framework/inc/pm_fg_sram.h#1 $
  $DateTime: 2015/07/02 04:11:47 $
  $Author: pwbldsvc $

 when            who     what, where, why
 --------------------------------------------------------------------------------------
 09/22/14   aab     Updated to support FG SRAM configuration in SBL
 06/06/14   va      New file.
=============================================================================*/

/**
  PMIC Platform interface
 */

#include "com_dtypes.h"         /* Containse basic type definitions */
#include "pm_err_flags.h"     /* Containse the error definitions */
#include "pm_resources_and_types.h"

// TINNO BEGIN
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#include "pm_app_smbchg.h"
// TINNO END

/*===========================================================================
                               MACROS
===========================================================================*/

#define BATTARY_PROFILE_MAX_SIZE 0x80 /* 128 bytes */
#define FG_SRAM_TEMP_REGISTER_ADDRESS 0x05D0 // Refer to scratch pad register table start from 0x540 - 0x5FF
#define FG_SRAM_PROFILE_START_ADDRESS 0x4C0
#define FG_SRAM_PROFILE_END_ADDRESS   0x53F

#define NUM_BITS_IN_BYTE sizeof(uint8) * 8
#define NUM_BYTE_IN_WORD sizeof(uint32)
#define FG_SRAM_RD_WR_BUS_WIDTH         4
#define FG_SRAM_RD_WR_OFFSET_WIDTH      3

#define FG_SRAM_MAX_SIZE 0x1FF /* Absoulte Sram Max mMemory Size */

#define SBL_SRAM_CONFIG_SIZE 7

/*=========================================================================
                            GLOBAL VARIABLES
===========================================================================*/
/**
  Sram State
*/
typedef enum _FgSramState{
  FG_SRAM_STATUS_INIT,
  FG_SRAM_STATUS_LOAD_PROFILE,
  FG_SRAM_STATUS_IN_USE,
  FG_SRAM_STATUS_POLLING,
  FG_SRAM_STATUS_AVAILABLE,
  FG_SRAM_STATUS_INVALID
}FgSramState;

/**
  Sram Address Data Pair
  Structure to hold address, data which would be updated in single Sram access
*/

typedef struct {
  uint32 SramAddr;
  uint32 SramData;
  //UINT32 SramAddrDataCount;//Reserved
}FgSramAddrData;

/**
  Sram Address Data Pair with Data Offset and Size
  Structure to hold address, data which would be updated in single Sram access
*/

typedef struct {
  uint32 SramAddr;
  uint32 SramData;
  uint8  DataOffset;   //Offset from Sram Address given valid values 0 -3
  uint8  DataSize;     //Number of bytes to write (0 - 4): Skip configuring if DataSize=0
  pm_smbchg_param_config_enable_type  EnableConfig; //PM_DISABLE_CONFIG = 0, PM_ENABLE_CONFIG  = 1,
}FgSramAddrDataEx_type;

/**
  Sram Address Data Pair with UINT8 data and offset 
  Structure to hold Address, Data (UINT8), Offset of provide data which would be updated in single Sram access
*/


typedef struct {
  uint32 SramAddr;//Sram Start Address for the given offset 
  uint8  SramData;//uint8 Data
  uint8  DataOffset;//Offset from Sram Address given valid values 0 -3
}FgSramAddrDataOffset;


/*===========================================================================
                               FUNCTION DEFINITIONS
===========================================================================*/

pm_err_flag_type PmicFgSram_Init(FgSramState FgSramState);

pm_err_flag_type PmicFgSram_GetState(FgSramState *FgSramSt);

//Single Check on SRAM memory access
pm_err_flag_type PmicFgSram_Dump(uint32  PmicDeviceIndex, uint32 DumpSramStartAddr,
                                   uint32 DumpSramEndAddr);

pm_err_flag_type PmicFgSram_ProgBurstAccessEx(uint32 PmicDeviceIndex, FgSramAddrDataEx_type * AddrDataPairEx, uint32 Count);

pm_err_flag_type PmicFgSram_ProgBurstAccess(uint32 PmicDeviceIndex, FgSramAddrData * AddrDataPair,
                                                 uint32 Count);

// TINNO BEGIN
// Added by liaoye on Nov. 22, 2016 for: add function--battery temperature check during bootup
#ifdef FEATURE_CHECK_TEMPERATURE_BOOTUP
pm_err_flag_type PmicFgSram_get_batt_temp_raw(uint32 PmicDeviceIndex, uint32 DumpSramStartAddr, uint32 DumpSramEndAddr, uint32 *raw);
#endif
// TINNO END

pm_err_flag_type PmicFgSram_ProgSingleAccess(uint32 PmicDeviceIndex, FgSramAddrDataOffset * AddrDataPair,
                                                 uint32 AddrDataCount);

#endif //__PMICFGSRAM_H__

