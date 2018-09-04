RECSIZE=80;BLOCKFACTOR=16;CODE=0;EXTENTS=11;FILESIZE=329;ASCII;FORMAT=F�$CONTROL STANDARD_LEVEL SYSTEM, LIST ON ,SHORT                          00001000C   $CODE_OFFSETS ON                                                    00002000C---$CHECK_ACTUAL_PARM 2                                                00003000      PROGRAM LISTENER                                                  00004000      IMPLICIT NONE                                                     00005000                                                                        00005100      COMMON /ABC/XYZ                                                   00005200      INTEGER*2 XYZ                                                     00005300      CALL ABOUT                                                        00006100      CALL MAIN                                                         00007000      STOP                                                              00008000      END                                                               00009000                                                                        00010000                                                                        00011000      SUBROUTINE TOUPPER( ST, BOST, EOST )                              00012000      IMPLICIT NONE                                                     00013000                                                                        00014000      CHARACTER ST*80                                                   00015000      INTEGER*2 BOST, EOST                                              00016000      INTEGER*2 I,IBUF                                                  00017000      CHARACTER BUF*2                                                   00018000                                                                        00019000      EQUIVALENCE ( BUF, IBUF )                                         00020000                                                                        00021000      DO I=BOST,EOST                                                    00022000        IF ( ST(I:I) .GE. 'a' .AND. ST(I:I) .LE. 'z' ) THEN             00023000          BUF(2:2) = ST(I:I)                                            00024000          IBUF = IBUF - 32                                              00025000          ST(I:I) = BUF(2:2)                                            00026000        ENDIF                                                           00027000      ENDDO                                                             00028000                                                                        00029000      RETURN                                                            00030000      END                                                               00031000                                                                        00032000                                                                        00033000      SUBROUTINE PARSE( ST, L, BOKEY, EOKEY, BOVALUE, EOVALUE )         00034000      IMPLICIT NONE                                                     00035000                                                                        00036000      CHARACTER ST*80                                                   00037000      INTEGER*2 BOKEY, EOKEY, BOVALUE, EOVALUE                          00038000      INTEGER*2 I,L                                                     00039000                                                                        00040000      BOKEY=0                                                           00041000      EOKEY=0                                                           00042000      BOVALUE=0                                                         00043000      EOVALUE=0                                                         00044000                                                                        00045000      IF ( L .GT. 80 ) L = 80                                           00045100      IF ( L .EQ. 0 ) RETURN                                            00045110                                                                        00045200      I=1                                                               00046000C     DO WHILE ( ST(I:I) .EQ. " " .AND. I .LT. 80 )                     00047000      DO WHILE ( ST(I:I) .EQ. " " .AND. I .LT. L )                      00047100        I = I + 1                                                       00048000      ENDDO                                                             00049000      BOKEY = I                                                         00050000      EOKEY = I                                                         00051000                                                                        00052000C     DO WHILE ( ST(I:I) .NE. "=" .AND. I .LT. 80 )                     00053000      DO WHILE ( ST(I:I) .NE. "=" .AND. I .LT. L )                      00053100        I = I + 1                                                       00054000        IF ( ST(I:I) .NE. " " .AND. ST(I:I) .NE. "=" ) EOKEY = I        00055000      ENDDO                                                             00056000                                                                        00057000C     IF ( I .LT. 80 .AND. ST(I:I) .EQ. "#" ) RETURN                    00057100      IF ( I .LT. L .AND. ST(I:I) .EQ. "#" ) RETURN                     00057110                                                                        00057200      I = I + 1                                                         00058000C     DO WHILE ( ST(I:I) .EQ. " " .AND. I .LT. 80 )                     00059000      DO WHILE ( ST(I:I) .EQ. " " .AND. I .LT. L )                      00059100        I = I + 1                                                       00060000      ENDDO                                                             00061000      BOVALUE = I                                                       00062000      EOVALUE = I                                                       00063000                                                                        00064000C     DO WHILE ( I .LT. 80 )                                            00065000      DO WHILE ( I .LT. L )                                             00065100        I = I + 1                                                       00066000        IF ( ST(I:I) .NE. " " ) EOVALUE = I                             00067000      ENDDO                                                             00068000                                                                        00069000      RETURN                                                            00070000      END                                                               00071000                                                                        00072000      SUBROUTINE PROPERTY( FNAME, KEY, VALUE, IERR )                    00073000      IMPLICIT NONE                                                     00074000                                                                        00075000      SYSTEM INTRINSIC FREAD, FOPEN, COMMAND, FCHECK, FERRMSG, FCLOSE   00076000                                                                        00077000      INTEGER*2 IERR, IERR1, IERR2                                      00078000      INTEGER*2 FNUM, FOP, AOP, ERRL, L, I                              00079000      INTEGER*2 BOKEY, EOKEY, BOVALUE, EOVALUE                          00080000      LOGICAL FOPT,AOPT,FMSGL(40),REC(40)                               00081000      CHARACTER KEY*(*), VALUE*80                                       00082000      INTEGER*2 KEYL                                                    00083000      CHARACTER COMMST*80,FNAME*(*),FMSG*80,RECST*80,PASSWORD*80        00084000      LOGICAL DONE                                                      00085000                                                                        00086000      EQUIVALENCE (FOP,FOPT),(AOP,AOPT)                                 00087000      EQUIVALENCE (FMSG,FMSGL),(RECST,REC)                              00088000                                                                        00089000      IERR = 0                                                          00089100                                                                        00089200      KEYL = LEN(KEY)                                                   00090000      DONE = .FALSE.                                                    00091000      DO WHILE ( .NOT. DONE )                                           00092000        IF ( KEY(KEYL:KEYL) .NE. " " ) THEN                             00093000          DONE = .TRUE.                                                 00094000        ELSE                                                            00095000          KEYL = KEYL - 1                                               00096000        ENDIF                                                           00097000      ENDDO                                                             00098000      CALL TOUPPER( KEY, 1, KEYL )                                      00099000                                                                        00100000      FOP = 1B                                                          00101000      AOP = 5B                                                          00102000                                                                        00103000      FNUM = FOPEN( FNAME, FOPT, AOPT )                                 00104000                                                                        00105000      IF ( FNUM .LE. 0 ) THEN                                           00106000        CALL FCHECK( FNUM, IERR1 )                                      00107000C       PRINT *,"FNUM IERR1=",IERR1                                     00108000C       CALL FERRMSG( IERR1, FMSGL, ERRL )                              00109000C       PRINT *,"MSG=",FMSG                                             00110000      ELSE                                                              001110005       CONTINUE                                                        00112000        L = FREAD(FNUM,REC,-80)                                         00113000        IF ( CCODE() ) 100,10,100                                       0011400010      CONTINUE                                                        00115000        IF ( L .GT. 0 ) L = L * 2                                       00115010        IF ( L .LT. 0 ) L = L * -1                                      00115100        CALL PARSE( RECST, L, BOKEY, EOKEY, BOVALUE, EOVALUE )          00116000        IF ( KEYL .EQ. EOKEY - BOKEY + 1 ) THEN                         00117000          CALL TOUPPER( RECST, BOKEY, EOKEY )                           00118000          IF ( KEY(1:KEYL) .EQ. RECST(BOKEY:EOKEY) ) THEN               00119000            VALUE = "            "                                      00120000            VALUE(1:EOVALUE - BOVALUE + 1) = RECST(BOVALUE:EOVALUE)     00121000            GOTO 200                                                    00122000          ENDIF                                                         00123000        ENDIF                                                           00124000        GOTO 5                                                          00125000      ENDIF                                                             00126000                                                                        00127000100   CONTINUE                                                          00128000      IERR = -1                                                         00128010200   CONTINUE                                                          00129000                                                                        00130000      CALL FCLOSE( FNUM, 0B, 0B )                                       00131000                                                                        00132000      RETURN                                                            00133000      END                                                               00134000                                                                        00135000                                                                        00136000      LOGICAL FUNCTION ISERROR( RESULT )                                00137000      IMPLICIT NONE                                                     00138000                                                                        00139000      SYSTEM INTRINSIC IPCERRMSG                                        00140000                                                                        00141000      INTEGER*4 RESULT, NEWRESULT, LN                                   00142000      CHARACTER ERR*80                                                  00143000                                                                        00144000      ISERROR = .FALSE.                                                 00145000      IF ( RESULT .NE. 0) THEN                                          00146000        PRINT *,"IPC ERROR, RESULT = ", RESULT                          00147000        CALL IPCERRMSG( RESULT, ERR, LN, NEWRESULT )                    00148000        PRINT *,"ERR=", ERR                                             00149000        ISERROR = .TRUE.                                                00150000      ENDIF                                                             00151000                                                                        00152000      RETURN                                                            00153000      END                                                               00154000                                                                        00155000                                                                        00156000      SUBROUTINE MAIN                                                   00157000      IMPLICIT NONE                                                     00158000                                                                        00159000      SYSTEM INTRINSIC IPCCREATE                                        00163000      SYSTEM INTRINSIC IPCSHUTDOWN, ADDOPT, INITOPT, READOPT            00164000      SYSTEM INTRINSIC IPCRECVCN, IPCGIVE                               00165000      SYSTEM INTRINSIC IPCCONTROL, IOWAIT                               00166000      SYSTEM INTRINSIC CREATEPROCESS                                    00166100                                                                        00170000      INTEGER*4 CD, RESULT, LN, VC, SNUM, VCNEW                         00171000      INTEGER*4 INT2                                                    00172000      INTEGER*2 RESULT16, PORT, OPT(7)                                  00174000      INTEGER*2 DATXLENGTH, OPTIONCODE, CSTATION, ERROR, LNGTH          00175000      INTEGER*2 II, L, GOODX                                            00176000      CHARACTER ERR*80, DATX*32                                         00178000      CHARACTER COPT*14, CPORT*2, CINT2*2                               00179000      CHARACTER PROCESS*27, VC_ST*10                                    00180000      CHARACTER PORTC*80                                                00181000      CHARACTER XL*80                                                   00181100      LOGICAL XLDEFINED                                                 00181200      INTEGER*2 IERR,PIN,PARM                                           00182000      INTEGER*4 STATUS, ITEMNUMS(10),ITEMS(10)                          00182100      LOGICAL ISERROR,REC(128)                                          00183000                                                                        00186000      EQUIVALENCE ( OPT, COPT ), ( PORT, CPORT ), (INT2, CINT2)         00187000                                                                        00192000      XLDEFINED = .FALSE.                                               00192100      CALL PROPERTY( "ADBCCONF", "XL", XL, IERR )                       00192200      IF ( IERR .EQ. 0 ) THEN                                           00192300        XLDEFINED = .TRUE.                                              00192400        PRINT *,"XL Path: XL=",XL                                       00192410      ENDIF                                                             00192500                                                                        00192600      CD = 0                                                            00195000      RESULT = 0                                                        00196000      RESULT16 = 0                                                      00197000      OPT(1) = 0                                                        00198000      OPT(2) = 0                                                        00199000      OPT(3) = 0                                                        00200000      OPT(4) = 0                                                        00201000      OPT(5) = 0                                                        00202000      OPT(6) = 0                                                        00203000      OPT(7) = 0                                                        00204000      CALL INITOPT (COPT,1,RESULT16)                                    00205000      IF (RESULT16.NE.0) PRINT *, "INITOPT FAILED"                      00206000                                                                        00206100      CALL PROPERTY( "ADBCCONF", "PORT", PORTC, IERR )                  00210100      IF ( IERR .EQ. 0 ) THEN                                           00210200        PORT = INUM(PORTC)                                              00210300      ELSE                                                              00210400C       PORT = 30803                                                    00210500        PORT = 30807                                                    00210510      ENDIF                                                             00210600      PRINT *,"Port: ",PORT                                             00210610C----------                                                             00211000      CALL ADDOPT (COPT,0,128,2,CPORT,RESULT16)                         00212000      IF (RESULT16.NE.0) PRINT *,"ADDOPT FAILED"                        00213000C---------------                                                        00214000      OPTIONCODE = 0                                                    00215000      DATXLENGTH = 32                                                   00216000      DATX = " "                                                        00217000      CALL READOPT (COPT,0,OPTIONCODE,DATXLENGTH,DATX,RESULT16)         00218000C     PRINT *,"RESULT: ", RESULT16," DATX: ", DATX                      00219000                                                                        00220000                                                                        00221000      CALL IPCCREATE( 3, 0,,COPT , CD, RESULT )                         00222000      IF ( ISERROR( RESULT ) ) GOTO 9999                                00223000                                                                        00224000      INT2 = 0                                                          00225000      CALL IPCCONTROL( CD, 3, CINT2, 2,,,,RESULT )                      00226000      IF ( ISERROR( RESULT ) ) GOTO 9999                                00227000                                                                        00228000      CALL IPCCONTROL( CD, 1,,,,,, RESULT )                             00229000      IF ( ISERROR( RESULT ) ) GOTO 9999                                00230000                                                                        00231000      CALL IPCRECVCN( CD, VCNEW,,, RESULT )                             00236000      IF ( ISERROR( RESULT ) ) GOTO 9999                                00237000                                                                        00238000      II = 0                                                            00252000      DO WHILE ( .TRUE. )                                               00253000C       II = II + 1                                                     00254000C       PRINT *,"PRIOR TO IOWAIT..."                                    00255000        SNUM = IOWAIT(0,REC,LNGTH,CSTATION)                             00256000        GOODX = CCODE()                                                 00257000        IF ( GOODX .NE. 0 ) THEN                                        00258000          PRINT *,"IOWAIT ERROR = ",GOODX                               00259000        ENDIF                                                           00260000        IF ( SNUM .EQ. CD ) THEN                                        00261000                                                                        00262000          PROCESS = "DATAMGR  "                                         00263000                                                                        00264000          II = II + 1                                                   00265000          IF ( II.EQ.32765 ) II = 1                                     00265100          WRITE(VC_ST,'("VC",I6.6)') II                                 00266000                                                                        00267000          INT2 = 0                                                      00268000          CALL IPCGIVE(VCNEW,VC_ST,8,INT2,RESULT)                       00269000          IF ( ISERROR(RESULT) ) GOTO 9999                              00270000                                                                        00271000          PARM = II                                                     00272000          ITEMNUMS(1) = 2     !PARM                                     00272100          ITEMS(1) = PARM                                               00272200          ITEMNUMS(2) = 10    !ACTIVATE                                 00272300          ITEMS(2) = 0                                                  00272400          ITEMNUMS(3) = 0                                               00272410          ITEMS(3) = 0                                                  00272420                                                                        00272430          IF ( XLDEFINED ) THEN                                         00272440C           PRINT *,"XLDEFINED BADDRESS(XL)=",BADDRESS(XL)              00272441C           PRINT *,"XLDEFINED REF",%LOC(XL)                            00272442            ITEMNUMS(3) = 19                                            00272450C           ITEMS(3) = BADDRESS( XL )                                   00272460            ITEMS(3) = %LOC(XL)                                         00272461            ITEMNUMS(4) = 24                                            00272470            ITEMS(4) = 80                                               00272480            ITEMNUMS(5) = 0                                             00272490            ITEMS(5) = 0                                                00272491          ENDIF                                                         00272492                                                                        00272500          CALL CREATEPROCESS(STATUS,PIN,PROCESS,ITEMNUMS,ITEMS)         00272510C---------CALL CR_PROCESS(PROCESS,PIN,PARM,IERR)                        00273000C---------IF ( IERR .NE. 0 ) THEN                                       00274000          IF ( STATUS .NE. 0 ) THEN                                     00274100            PRINT *,"COULD NOT CREATE PROCESS DATAMGR"                  00275000            PRINT *,"STATUS: ",STATUS                                   00275100          ENDIF                                                         00276000          CALL IPCRECVCN( CD, VCNEW,,, RESULT )                         00294000          IF ( ISERROR( RESULT ) ) GOTO 9999                            00295000        ENDIF                                                           00296000      ENDDO                                                             00297000                                                                        00298000      CALL IPCSHUTDOWN( VC )                                            00299000                                                                        003000009999  RETURN                                                            00301000      END                                                               00302000                                                                        00303000                                                                        00304000                                                                        00305000      SUBROUTINE CR_PROCESS(PROCESS,PIN,PARM,IERR)                      00306000      IMPLICIT NONE                                                     00307000                                                                        00308000      SYSTEM INTRINSIC CREATE,ACTIVATE                                  00309000      CHARACTER PROCESS*27, PTC*1                                       00310000      INTEGER*2 PIN,PARM, IERR                                          00311000                                                                        00312000      IERR = 0J                        !PASS PTC AS PARM VALUE.         00313000      CALL CREATE(PROCESS,,PIN,PARM,41B)                                00314000      IF (CCODE()) 997,8,8                                              003150008     CALL ACTIVATE(PIN)                                                00316000      IF (CCODE()) 997,9,9                                              003170009     RETURN                                                            00318000997   IERR = -9999J                                                     00319000      RETURN                                                            00320000      END                                                               00321000                                                                        00322000