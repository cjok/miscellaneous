#ifndef _BATTERY_TEMPERATURE_DATA_H
#define _BATTERY_TEMPERATURE_DATA_H

/* ============================================================*/
/* define*/
/* ============================================================*/
#define BAT_NTC_10 1
#define BAT_NTC_47 0

#if (BAT_NTC_10 == 1)
#define RBAT_PULL_UP_R	16900
#define RBAT_PULL_DOWN_R	27000
#endif

#if (BAT_NTC_47 == 1)
#define RBAT_PULL_UP_R	61900
#define RBAT_PULL_DOWN_R	100000
#endif
#define RBAT_PULL_UP_VOLT	1800

// battery temperature threshold
#define CLOD_TEMP_THRESLOD  (-20)
#define HOT_TEMP_THRESLOD   (60)

/* ============================================================*/
/* typedef*/
/* ============================================================*/
typedef struct _BATTERY_PROFILE_STRUCT {
	signed int percentage;
	signed int voltage;
} BATTERY_PROFILE_STRUCT, *BATTERY_PROFILE_STRUCT_P;

typedef struct _R_PROFILE_STRUCT {
	signed int resistance; /* Ohm*/
	signed int voltage;
} R_PROFILE_STRUCT, *R_PROFILE_STRUCT_P;

typedef struct {                                                                                                                                          
	signed int BatteryTemp;
  	signed int TemperatureR;
} BATT_TEMPERATURE;

/* ============================================================*/
/* <temperature, ntc resistance> Table*/
/* ============================================================*/
#if (BAT_NTC_10 == 1)
	BATT_TEMPERATURE Batt_Temperature_Table[] = {
        {-30,127476},
        {-25,96862},
        {-20,74354},
        {-15,57626},
        {-10,45068},
        { -5,35548},
        {  0,28267},
        {  5,22650},
        { 10,18280},
        { 15,14855},
        { 20,12151},
        { 25,10000},
        { 30,8279},
        { 35,6892},
        { 40,5768},
        { 45,4852},
        { 50,4101},
        { 55,3483},
        { 60,2970},
        { 65,2544},
        { 70,2188},
        { 75,1889},
        { 80,1637}
};
#endif

#if (BAT_NTC_47 == 1)
	BATT_TEMPERATURE Batt_Temperature_Table[] = {
		{-20, 483954},
		{-15, 360850},
		{-10, 271697},
		{ -5, 206463},
		{  0, 158214},
		{  5, 122259},
		{ 10, 95227},
		{ 15, 74730},
		{ 20, 59065},
		{ 25, 47000},
		{ 30, 37643},
		{ 35, 30334},
		{ 40, 24591},
		{ 45, 20048},
		{ 50, 16433},
		{ 55, 13539},
		{ 60, 11210}
	};
#endif

#endif

