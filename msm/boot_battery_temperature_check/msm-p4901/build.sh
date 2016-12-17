#!/bin/bash

# 在这里修改本机的环境变量
export HEXAGON_ROOT=~/Qualcomm/HEXAGON_Tools
export ARMROOT=~/ARM/ARM501bld94
export ARMHOME=$ARMROOT
export ARMINC=$ARMROOT/include
export ARMLIB=$ARMROOT/lib
export ARMBIN=$ARMROOT/bin
export ARMPATH=$ARMBIN
export ARM_COMPILER_FILE=$ARMBIN
export ARMINCLUDE=$ARMROOT/include
export PATH=$HEXAGON_ROOT:$PATH	
export PATH=$ARMINC:$ARMLIB:$ARMBIN:$ARMPATH:$ARMINCLUDE:$PATH	

#jenkins request
export DATE_INFO=$(date +%s)
export BUILD_NUMBER=eng.android.${DATE_INFO}

echo "BUILD_ID $BUILD_ID"
unset BUILD_ID

version=1.67
bldred='\e[1;31m'  # Red
txtrst='\e[0m'		 # Reset
bakwht='\e[47m'    # White
bldgrn='\e[1;32m'  # Green
On_White='\e[47m'  # White

echo -en $bldred
echo "$0 version:$version"
echo -en $txtrst  

# Redirect stdout ( > ) into a named pipe ( >() ) running "tee"
exec > >(tee build.log)
exec 2>&1


function usage()
{
	echo -en $bldred
	echo "usage:$0 [project] [ modem | boot | rpm | tz ] [clean]"
	echo "      project: support L5221 L5224 L5251 P4901 P4901_ALT P4903 P4901TK and so on"
	echo "      this script is not for build android."
	echo "      android build script is in LINUX/android/build.sh"
	echo -en $txtrst  
	exit
}
WORKD=$PWD
PROJECT="V3901"
NV_HW_CONFIG="DS"
NV_SW_CONFIG="Tinno"
MODULE="$1"
ACTION="$2"
RF_SRC_ORI=$PWD/modem_proc/custom/8909
RF_SRC="$RF_SRC_ORI"
RF_SRC_TARGET="modem_proc"

MBN_OTA_DEFAULT=1
MBN_OTA_LATAM_AMX=0
MBN_OTA_EU_DT=0
MBN_OTA_EU_EE=0
MBN_OTA_EU_Orange=0
MBN_OTA_EU_SFR=0
MBN_OTA_EU_Telefonica=0
MBN_OTA_EU_Vodafone=0
MBN_OTA_EU_Vodafone_Turkey=0
MBN_OTA_Korea_KT=0
MBN_OTA_Korea_LGU=0
MBN_OTA_Korea_SKT=0
MBN_OTA_Korea_TTA=0
MBN_OTA_Japan_KDDI=0
#test mbn
#MBN_OTA_CN_CU=1
#MBN_OTA_CN_CMCC=1

#如果第一个参数是PROJECT

if [ "V3903"x = "$MODULE"x ] || [ "v3903"x = "$MODULE"x ]; then
	PROJECT="V3903"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3903.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3903_BYG"x = "$MODULE"x ] || [ "v3903byg"x = "$MODULE"x ]; then
	PROJECT="V3903"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3903.wik.xml	
  MODULE="$2"
  ACTION="$3"	
  NV_SW_CONFIG="BYG"
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3903BN_SUNRISE"x = "$MODULE"x ] || [ "v3903bn_sunrise"x = "$MODULE"x ]; then
	PROJECT="V3903BN_SUNRISE"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3903.wik.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3903_WIK"x = "$MODULE"x ] || [ "v3903_wik"x = "$MODULE"x ]; then
	PROJECT="V3903"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3903_pm8909.xml
	export TINNO_BUILD_PARTITION_NAME=partition.3903.wik.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3903SS"x = "$MODULE"x ] || [ "v3903ss"x = "$MODULE"x ]; then
	PROJECT="V3903"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_t3903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3903.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3903SS_WIK"x = "$MODULE"x ] || [ "v3903ss_wik"x = "$MODULE"x ]; then
	PROJECT="V3903"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3903.wik.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3903
fi
if [ "V3901"x = "$MODULE"x ] || [ "v3901"x = "$MODULE"x ]; then
	PROJECT="V3901"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3901.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3901
fi
if [ "V3901SS"x = "$MODULE"x ] || [ "v3901ss"x = "$MODULE"x ]; then
	PROJECT="V3901"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3901.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3901
fi
if [ "V3901SS_MTN"x = "$MODULE"x ] || [ "v3901ss_mtn"x = "$MODULE"x ]; then
	PROJECT="V3901"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3901.mtn.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3901
fi
if [ "V3901_BQ_RU"x = "$MODULE"x ] || [ "v3901_bq_ru"x = "$MODULE"x ]; then
	PROJECT="V3901_BQ_RU"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.3901.xml	
  MODULE="$2"
  ACTION="$3"	
  RF_SRC=$PWD/modem_proc/custom/3901
fi
if [ "V3901_WIK"x = "$MODULE"x ] || [ "v3901_wik"x = "$MODULE"x ]; then
	PROJECT="V3901"
	export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml
	export TINNO_BUILD_PARTITION_NAME=partition.3901.wik.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/3901
fi
if [ "P4901"x = "$MODULE"x ] || [ "p4901"x = "$MODULE"x ]; then
	PROJECT="P4901"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4901.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4901
  MBN_OTA_EU_Vodafone_Turkey=1
fi
if [ "P4901_JP"x = "$MODULE"x ] || [ "p4901jp"x = "$MODULE"x ]; then
	PROJECT="P4901"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4901_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4901.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4901
  MBN_OTA_Japan_KDDI=1
fi
if [ "P4903_TK"x = "$MODULE"x ] || [ "p4903_tk"x = "$MODULE"x ]; then
	PROJECT="P4903_TK"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4901tk_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4901.tk.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903  
  MBN_OTA_EU_Vodafone_Turkey=1
fi
if [ "P4901_WIK"x = "$MODULE"x ] || [ "p4901_wik"x = "$MODULE"x ]; then
	PROJECT="P4901_WIK"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4901_pm8909.xml
	export TINNO_BUILD_PARTITION_NAME=partition.4901.wik.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4901
fi

if [ "P4901_ALT"x = "$MODULE"x ] || [ "p4901_alt"x = "$MODULE"x ]; then
	PROJECT="P4901_ALT"
	NV_HW_CONFIG="SS"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4901_pm8909.xml
	export TINNO_BUILD_PARTITION_NAME=partition.4901.wik.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4901
  MBN_OTA_EU_SFR=1
fi

if [ "P4903"x = "$MODULE"x ] || [ "p4903"x = "$MODULE"x ]; then
	PROJECT="P4903"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4903.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903
fi
if [ "P4903_NOAPT"x = "$MODULE"x ] || [ "p4903_noapt"x = "$MODULE"x ]; then
	PROJECT="P4903"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4903.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903_NOAPT
fi
if [ "P4903_PK"x = "$MODULE"x ] || [ "p4903_pk"x = "$MODULE"x ]; then
	PROJECT="P4903_PK"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4903.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903
fi
if [ "P4903_PH"x = "$MODULE"x ] || [ "p4903_ph"x = "$MODULE"x ]; then
	PROJECT="P4903_PH"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4903_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4903.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903
fi
if [ "P4903_JP"x = "$MODULE"x ] || [ "p4903_jp"x = "$MODULE"x ]; then
	PROJECT="P4903_JP"
	export TINNO_BUILD_CONTENTS_NAME=contents_p4903jp_pm8909.xml  
	export TINNO_BUILD_PARTITION_NAME=partition.4903.jp.xml
  MODULE="$2"
  ACTION="$3"
  RF_SRC=$PWD/modem_proc/custom/4903_JP
  MBN_OTA_Japan_KDDI=1
fi

echo -e "\n#define $PROJECT" > modem_proc/build/cust/tinno_oem.h

echo -en $bldred
echo PROJECT is $PROJECT
echo -en $txtrst

#如果第一个参数是clean
if [ "clean"x = "$MODULE"x ]; then
	MODULE="all"
	ACTION="clean"
	echo "clean every thing"
fi

#如果没有给出模块参数
if [ ""x = "$MODULE"x ]; then
	MODULE="all"
	echo "no parameters"
	usage
fi

echo -en $bldred
echo TINNO_BUILD_CONTENTS_NAME is $TINNO_BUILD_CONTENTS_NAME
echo -en $txtrst
if [ ""x = "$TINNO_BUILD_CONTENTS_NAME"x ]; then
  export TINNO_BUILD_CONTENTS_NAME=contents_v3901_pm8909.xml  
  echo -en $bldred
  echo TINNO_BUILD_CONTENTS_NAME change to $TINNO_BUILD_CONTENTS_NAME
  echo -en $txtrst
fi

echo -en $bldred
echo TINNO_BUILD_PARTITION_NAME is $TINNO_BUILD_PARTITION_NAME
echo -en $txtrst
if [ ""x = "$TINNO_BUILD_PARTITION_NAME"x ]; then
  export TINNO_BUILD_PARTITION_NAME=partition.3901.xml
  echo -en $bldred
  echo TINNO_BUILD_PARTITION_NAME change to $TINNO_BUILD_PARTITION_NAME
  echo -en $txtrst
fi

ANDROIDOUT=$PWD/LINUX/android/out/target/product/msm8909/
SBLF=$WORKD/boot_images/build/ms/bin/8909/emmc/sbl1.mbn            
RPMF=$WORKD/rpm_proc/build/ms/bin/8909/pm8916/rpm.mbn
TZF=$WORKD/trustzone_images/build/ms/bin/MAZAANAA/tz.mbn
TZCF=$WORKD/trustzone_images/build/ms/setenv.sh
MODEMF=$WORKD/modem_proc/build/ms/bin/8909.gen.prod/qdsp6sw.mbn
NHLOSF=$WORKD/common/build/bin/asic/NON-HLOS.bin

#mkdir -p $ANDROIDOUT

function file_assert()
{
	filename=$1
	if [ ! -f "$filename" ]; then
		echo -en $bldred
	  echo "file is not exist. $filename"
		echo -en $txtrst	  
	  exit -1
	else
	  echo "file check ok. $filename"
	fi
}
function dir_assert()
{
	filename=$1
	if [ ! -d "$filename" ]; then
		echo -en $bldred
	  echo "dir is not exist. $filename"
		echo -en $txtrst  
	  exit -1
	else
	  echo "dir check ok. $filename"
	fi
}

function cpv()
{
	echo -en $bldgrn
	echo "cp $1 >>> $2 "
	echo -en $txtrst	
	cp $1 $2
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cp $1 $2 failed"
		echo -en $txtrst	
		exit -1
	fi	
}

function update_non_hlos()
{
	rm $NHLOSF

	TARGET_PATH="common/build"
	cd $TARGET_PATH
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cd $TARGET_PATH failed"
		echo -en $txtrst	
		exit -1
	fi
	
	echo PWD:$PWD
	
	if [ "clean"x != "$ACTION"x ] ; then
		echo "build NON-HLOS"		
		python update_common_info.py
	fi

	cd $WORKD	
	
	file_assert $NHLOSF
	#cpv $NHLOSF $ANDROIDOUT
	
}

	
dir_assert $HEXAGON_ROOT
dir_assert $ARMROOT
dir_assert $ARMINC
dir_assert $ARMLIB
dir_assert $ARMBIN
dir_assert $ARMINCLUDE


if 	[ "all"x != "$MODULE"x ] && 
		[ "modem"x != "$MODULE"x ] && 
		[ "boot"x != "$MODULE"x ] && 
		[ "tz"x != "$MODULE"x ] && 
		[ "trustzone"x != "$MODULE"x ]  && 
		[ "HLOS"x != "$MODULE"x ] &&
		[ "NON-HLOS"x != "$MODULE"x ] &&
		[ "non-hlos"x != "$MODULE"x ] &&
		[ "rpm"x != "$MODULE"x ] &&
		[ "sbl"x != "$MODULE"x ]; 
then
	usage
fi 

#!!! modem !!!
export ARMTOOLS=NONE	#只有modem是用HEXAGON编译的
if [ "modem"x = "$MODULE"x ] || [ "all"x = "$MODULE"x ] ; then	
	#make a correct default nvram
	MCFG_DEF_D=modem_proc/mcfg/mcfg_gen/generic/common/Default
	cp -vf $MCFG_DEF_D/mcfg_hw_gen_Default_Original.xml $MCFG_DEF_D/mcfg_hw_gen_Default.xml
	cp -vf $MCFG_DEF_D/mcfg_sw_gen_Default_Original.xml $MCFG_DEF_D/mcfg_sw_gen_Default.xml
	cp -vf $MCFG_DEF_D/mcfg_hw_gen_Default_$NV_HW_CONFIG.xml $MCFG_DEF_D/mcfg_hw_gen_Default.xml
	cp -vf $MCFG_DEF_D/mcfg_sw_gen_Default_$NV_SW_CONFIG.xml $MCFG_DEF_D/mcfg_sw_gen_Default.xml
    
	#make rfdriver link
	echo "RF SRC:" $RF_SRC
	
	#remove the old rf source
	rm -vrf $RF_SRC_TARGET/rfc_jolokia
	rm -vrf $RF_SRC_TARGET/rfdevice_asm
	rm -vrf $RF_SRC_TARGET/rfdevice_pa

	#same as clean
	echo "copy :" $RF_SRC_ORI " -> " $RF_SRC_TARGET	  
	cp -vrf $RF_SRC_ORI/* $RF_SRC_TARGET/
	
	if [ "clean"x != "$ACTION"x ] ; then 
	  echo "copy :" $RF_SRC " -> " $RF_SRC_TARGET	  
	  cp -vrf $RF_SRC/* $RF_SRC_TARGET/
	fi
	
	TARGET_PATH="modem_proc/build/ms"
	cd $TARGET_PATH
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cd $TARGET_PATH failed"
		echo -en $txtrst	
		exit -1
	fi
	echo PWD:$PWD
	
	rm $MODEMF
	
	if [ "clean"x = "$ACTION"x ] ; then
		echo "clean modem"	
		./build.sh 8909.gen.prod -k -c
	else
		echo "build modem"		
		./build.sh 8909.gen.prod -k
		file_assert $MODEMF
	fi	
	
	cd $WORKD	
fi

#!!! sbl !!!
export ARMTOOLS=ARMCT5.01

if [ "boot"x = "$MODULE"x ] || [ "sbl"x = "$MODULE"x ] || [ "all"x = "$MODULE"x ] ; then	
	TARGET_PATH="boot_images/build/ms"
	cd $TARGET_PATH
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cd $TARGET_PATH failed"
		echo -en $txtrst	
		exit -1
	fi
	
	echo PWD:$PWD
	
	rm $SBLF
	
	if [ "clean"x = "$ACTION"x ] ; then
		echo "clean boot"	
		./build.sh TARGET_FAMILY=8909 --prod -c
	else
		echo "build boot"		
		./build.sh TARGET_FAMILY=8909 --prod USES_FLAGS=USES_BOOT_BATT_TEMP_CHECK
		
		file_assert $SBLF	
		cd $WORKD		
	fi
	
	cd $WORKD
	

fi

#!!! tz !!!
if [ "tz"x = "$MODULE"x ] || [ "all"x = "$MODULE"x ]; then	
	TARGET_PATH="trustzone_images/build/ms"
	cd $TARGET_PATH
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cd $TARGET_PATH failed"
		echo -en $txtrst	
		exit -1
	fi
	
	echo PWD:$PWD
	
	rm $TZCF  #this config file casue build failed.
	rm $TZF
	
	if [ "clean"x = "$ACTION"x ] ; then
		echo "clean trustzone"	
		#./build.sh CHIPSET=msm8909 tz sampleapp tzbsp_no_xpu playready widevine isdbtmm aostlm securitytest keymaster commonlib -c
		./build.sh CHIPSET=msm8909 tz sampleapp tzbsp_no_xpu playready widevine keymaster commonlib -c
	else
		echo "build trustzone"		
		#./build.sh CHIPSET=msm8909 tz sampleapp tzbsp_no_xpu playready widevine isdbtmm aostlm securitytest keymaster commonlib
		./build.sh CHIPSET=msm8909 tz sampleapp tzbsp_no_xpu playready widevine keymaster commonlib 


		file_assert $TZF
		cd $WORKD
	fi
	
	cd $WORKD
	
fi

#!!! rpm !!!
export ARMTOOLS=RVCT41
if [ "rpm"x = "$MODULE"x ] || [ "all"x = "$MODULE"x ] ; then	
	TARGET_PATH="rpm_proc/build"
	cd $TARGET_PATH
	if [ $?	-ne 0 ]; then
		echo -en $bldred
		echo "cd $TARGET_PATH failed"
		echo -en $txtrst	
		exit -1
	fi
	
	echo PWD:$PWD
	
	if [ "clean"x = "$ACTION"x ] ; then
		echo "clean rpm"	
		./build_8909.sh -c
	else
		echo "build rpm"		
		./build_8909.sh 	
		file_assert $RPMF
	fi
	
	cd $WORKD
fi

#!!! HLOS !!!
if [ "HLOS"x = "$MODULE"x ] ; then	
	ANDROIDD=$PWD/../baselineone

	file_assert $ANDROIDOUT/boot.img
	file_assert $ANDROIDOUT/recovery.img
	file_assert $ANDROIDOUT/system.img
	file_assert $ANDROIDOUT/cache.img
	file_assert $ANDROIDOUT/persist.img
	file_assert $ANDROIDOUT/userdata.img
	file_assert $ANDROIDOUT/emmc_appsboot.mbn
	file_assert $NHLOSF
	file_assert $SBLF
	file_assert $RPMF
	file_assert $TZF
	
	echo -n "copy file ."
	

	#for fastboot
	cpv $NHLOSF $ANDROIDOUT
	echo -n "."	
	cpv $SBLF $ANDROIDOUT
	echo -n "."
fi

if [ "NON-HLOS"x = "$MODULE"x ] || [ "non-hlos"x = "$MODULE"x ] || [ "all"x = "$MODULE"x ] ; then	

  echo PWD:$PWD
  
  cp -vf wcnss_proc/copy_folder/$TINNO_BUILD_CONTENTS_NAME .
  
  #some mbn copy
  rm -vrf modem_proc/mbn_ota/*
  mkdir modem_proc/mbn_ota/

  if [ "clean"x = "$ACTION"x ] ; then
    rm $NHLOSF  
  else
  
	  if [ $MBN_OTA_DEFAULT -eq "1" ] ; then
	  	cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/common/ROW/Tinno/mcfg_sw.mbn     modem_proc/mbn_ota/
	  fi	  
		if [ $MBN_OTA_LATAM_AMX -eq "1" ] ; then
	    cp -vrf modem_proc/mcfg/configs/mcfg_sw/generic/LATAM/AMX/Tinno/   modem_proc/mbn_ota/AMX/
    fi
		if [ $MBN_OTA_EU_DT -eq 1 ] ; then 
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/DT/						modem_proc/mbn_ota/DT/
	  fi
		if [ $MBN_OTA_EU_EE -eq 1 ] ; then 
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/EE/						modem_proc/mbn_ota/EE/
	  fi
		if [ $MBN_OTA_EU_Orange -eq 1 ] ; then 
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/Orange/				modem_proc/mbn_ota/Orange/
	  fi
	  if [ $MBN_OTA_EU_SFR -eq 1 ] || [ $MBN_OTA_EU_SFR -eq "1" ]; then 
                mkdir -p modem_proc/mbn_ota/SFR/FRA
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/SFR/VoLTE/FRA/*				modem_proc/mbn_ota/SFR/FRA
		echo "copy SFR mbn"
	  fi 
		if [ $MBN_OTA_EU_Telefonica -eq 1 ] ; then 
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/Telefonica/		modem_proc/mbn_ota/Telefonica/
	  fi
		if [ $MBN_OTA_EU_Vodafone -eq 1 ] ; then 
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/Vodafone/			modem_proc/mbn_ota/Vodafone/
	  fi
  	if [ $MBN_OTA_EU_Vodafone_Turkey -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Vodafone/Turkey/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/EU/Vodafone/VoLTE_TN/Turkey/*			modem_proc/mbn_ota/Vodafone/Turkey/
	  fi
  	if [ $MBN_OTA_CN_CU -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/CN/CU/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/China/CU/Commercial_TN/OpenMkt/*		modem_proc/mbn_ota/CN/CU/
	  fi	  
  	if [ $MBN_OTA_CN_CMCC -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/CN/CMCC/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/China/CMCC/Commercial_TN/NoV_OpenMkt/*		modem_proc/mbn_ota/CN/CMCC/
	  fi	  
  	if [ $MBN_OTA_Korea_KT -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Korea/KT/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/Korea/KT/Commercial_KT_LTE_TN/*		modem_proc/mbn_ota/Korea/KT/
	  fi
  	if [ $MBN_OTA_Korea_LGU -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Korea/LGU/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/Korea/LGU/Commercial_LGU_VoLTE_TN/*		modem_proc/mbn_ota/Korea/LGU/
	  fi
  	if [ $MBN_OTA_Korea_SKT -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Korea/SKT/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/Korea/SKT/Commercial_SKT_VoLTE_TN/*		modem_proc/mbn_ota/Korea/SKT/
	  fi
  	if [ $MBN_OTA_Korea_TTA -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Korea/TTA/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/Korea/TTA/Commercial_TN/*		modem_proc/mbn_ota/Korea/TTA/
	  fi
        if [ $MBN_OTA_Japan_KDDI -eq 1 ] ; then 
  		mkdir -p modem_proc/mbn_ota/Japan/KDDI/
	    cp -vrf  modem_proc/mcfg/configs/mcfg_sw/generic/APAC/KDDI/KDDI_Volte/*		modem_proc/mbn_ota/Japan/KDDI/
	  fi
	  	  
		echo `date +"%Y/%m/%d %H:%M:%S"` > modem_proc/mbn_ota/ver_info.txt
	  
	  rm -rvf common/build/bin/*
	  
	  update_non_hlos
	fi
fi
echo -e $bldgrn
echo "all done."
echo -en $txtrst	
