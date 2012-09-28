/*=================================================================
  Copyright (c) 2003 by CHIPNUTS Incorporated. All Rights Reserved.
  FileName: chp_comdef.h
  
  Author: Jerry       Date: 05/31/2006

  Description: header file for common definition.
  Version:     V1.0
  
  History:     
  <author>  <time>       <version>        <desc>    
  Jerry        05/31/2007     1.0        Initial Version  
  baggio	   2007-10-31	      1.1  	     modify  the architecture
==================================================================*/
#ifndef _CHP_COMDEF_H
#define _CHP_COMDEF_H

#ifndef NULL
#define NULL 		0
#endif

#define CHP_U8		unsigned char
#define CHP_U16		unsigned short
#define CHP_U32		unsigned long
#define CHP_U64		__int64

#define CHP_8		signed char
#define CHP_16		signed short
#define CHP_32		signed long

#define CHP_FILE			int

/*Return value type of functions*/
#define CHP_RTN_T	unsigned int

enum 
{ 
	CHP_RTN_SUCCESS,
	CHP_RTN_MEM_INIT_FAIL,
	CHP_RTN_MEM_MALLOC_FAIL
};



typedef void *(*CHP_MALLOC_FUNC)(CHP_U32);
typedef void (*CHP_FREE_FUNC)(void *); 
typedef void *(*CHP_MEMSET)(void *, CHP_32 c, CHP_U32);
typedef void *(*CHP_MEMCPY)(void *, const void *, CHP_U32);


typedef struct
{
	CHP_MALLOC_FUNC 	chp_malloc;
	CHP_FREE_FUNC 		chp_free;
	CHP_MEMSET			chp_memset;
	CHP_MEMCPY			chp_memcpy;
}CHP_MEM_FUNC_T;




/*switch little endian long long to local endian long long, x is a char pointer*/
#define PLE64TOCPU(X)   ((CHP_U64)(*(X+7))<<56 | (CHP_U64)(*(X+6))<<48 \
				|(CHP_U64)(*(X+5))<<40 | (CHP_U64)(*(X+4))<<32 \
				|(CHP_U64)(*(X+3))<<24 | (CHP_U64)(*(X+2))<<16 \
		              | (CHP_U64)(*(X+1))<<8 | *(X))


/*switch little endian long to local endian long, x is a char pointer*/
#define PLE32TOCPU(X)   ((CHP_U32)(*(X+3))<<24 | (CHP_U32)(*(X+2))<<16 \
		             | (CHP_U32)(*(X+1))<<8 | *(X))
		             
/*switch little endian short to local endian short, x is a char pointer*/
#define PLE16TOCPU(X)   ((CHP_U16)(*(X+1))<<8 | *(X))

/*switch big endian long to local endian long, x is a char pointer*/
#define PBE32TOCPU(X)   ((CHP_U32)(*(X))<<24 | (CHP_U32)(*(X+1))<<16 \
		             | (CHP_U32)(*(X+2))<<8 | *(X+3))

/*switch big endian short to local endian short, x is a char pointer*/
#define PBE16TOCPU(X)   ((CHP_U16)(*(X))<<8 | *(X+1))


extern CHP_RTN_T chp_mapi_mem_init(CHP_U32 *p_buf, CHP_U32 buf_size);


#endif
