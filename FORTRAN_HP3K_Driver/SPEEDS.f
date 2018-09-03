RECSIZE=80;BLOCKFACTOR=3;CODE=1052;EXTENTS=1;FILESIZE=99;ASCII;FORMAT=F�001000$CONTROL DYNAMIC                                                          001100 IDENTIFICATION DIVISION.                                                 001200 PROGRAM-ID.   SPEEDRACER.                                                001300 AUTHOR.       MGR-ADBC          .                                        001400                                                                          001500                                                                          001600 ENVIRONMENT DIVISION.                                                    001700 CONFIGURATION SECTION.                                                   001800 SOURCE-COMPUTER.   HP-3000.                                              001900 OBJECT-COMPUTER.   HP-3000.                                              002000 SPECIAL-NAMES.                                                           002100 CONDITION-CODE IS RETURN-CODE.                                           002200                                                                          002300$PAGE                                                                     002400 DATA DIVISION.                                                           002500 WORKING-STORAGE SECTION.                                                 002600 01  BASE                    PIC S9(4) COMP VALUE 0.                      002700                                                                          002800 01  PASSWORD                PIC X(7)  VALUE                              002900           "READER;".                                                     003000 01  DATA-SET                     PIC X(11)  VALUE                        003100          "COMPOSERS;".                                                   003200 01  COMPOSERSBUFF                PIC X(168)  VALUE SPACES.               003300 01  ALL-LIST                      PIC X(2)  VALUE "@".                   003400 01  DUMMY                         PIC X(6)  VALUE SPACES.                003500 01 STAT.                                                                 003600     03  C-WORD                    PIC S9(4) COMP VALUE 0.                003700     03  STAT2                     PIC S9(4) COMP VALUE 0.                003800     03  STAT3-4                   PIC S9(9) COMP VALUE 0.                003900     03  STAT5-6                   PIC S9(9) COMP VALUE 0.                004000     03  STAT7-8                   PIC S9(9) COMP VALUE 0.                004100     03  STAT9-10                  PIC S9(9) COMP VALUE 0.                004200 01  MODE1                         PIC S9(4) COMP VALUE 1.                004300 01  MODE2                         PIC S9(4) COMP VALUE 2.                004400                                                                          004500 01  dTRANSACTION-NUMBER          PIC S9(4) COMP.                         004600 01  dSCREEN-NAME                 PIC X(16).                              004700 01  dPRICE                       PIC S9(4) COMP.                         004800 01  dRETURN-STATUS               PIC S9(4) COMP.                         004900 01  dMAINT-EXPENSE               PIC S9(4) COMP.                         005000$PAGE                                                                     005100 LINKAGE SECTION.                                                         005200                                                                          005300*****                                                                     005400* USER IDENTIFICATION BUFFERS.                                            005500*****                                                                     005600                                                                          005700 01  PASS-DB                     PIC S9(4) COMP.                          005800 01  DATA-SET-NAME               PIC X(16).                               005900 01  DATA-ITEM-NAME              PIC X(16).                               006000 01  KEY-VALUE                   PIC X(18).                               006100 01  RETURN-CHAIN-RECS           PIC S9(9) COMP.                          006200$PAGE                                                                     006300                                                                          006400 PROCEDURE DIVISION USING PASS-DB,                                        006500                          DATA-SET-NAME,                                  006600                          DATA-ITEM-NAME,                                 006700                          KEY-VALUE,                                      006800                          RETURN-CHAIN-RECS.                              006900                                                                          007000**************************************                                    007100* MAIN PROGRAM FLOW                                                       007200**************************************                                    007300                                                                          007400 A000-MAIN-DRIVER-PARAGRAPH.                                              007500                                                                          007600      PERFORM B000-INITIALIZE THRU B999-EXIT.                             007700                                                                          007800      PERFORM C000-PROCESS-THINGS THRU C999-EXIT.                         007900                                                                          008000      GOBACK.                                                             008100                                                                          008200 A999-EXIT.                                                               008300     EXIT.                                                                008400                                                                          008500 B000-INITIALIZE.                                                         008600                                                                          008700     MOVE PASS-DB TO BASE.                                                008800                                                                          008900 B999-EXIT.                                                               009000     EXIT.                                                                009100                                                                          009200 C000-PROCESS-THINGS.                                                     009300                                                                          009400     CALL "DBFIND" USING BASE                                             009500                        DATA-SET-NAME                                     009600                        MODE1                                             009700                        STAT                                              009800                        DATA-ITEM-NAME                                    009900                        KEY-VALUE.                                        010000     IF C-WORD <> 0                                                       010100          MOVE -1 TO RETURN-CHAIN-RECS                                    010200          CALL "DBEXPLAIN" USING STAT                                     010300          GO TO C999-EXIT.                                                010400                                                                          010500     MOVE STAT5-6 TO RETURN-CHAIN-RECS.                                   010600                                                                          010700 C999-EXIT.                                                               010800    EXIT.                                                                 