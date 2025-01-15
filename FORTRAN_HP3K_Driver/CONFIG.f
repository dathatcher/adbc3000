RECSIZE=80;BLOCKFACTOR=3;CODE=0;EXTENTS=1;FILESIZE=163;ASCII;FORMAT=Fÿ{                                                                       
00001000    Property of : Advanced Network Systems, Inc (ANSI)                  
00002000                  13 Bucanan Way                                        
00003000                  Flemington, NJ 08822                                  
00004000                  (908) 237-1700                                       
00005000                                                                       
00006000         Copyrigth (c) 1998 All rights reserved                        
00007000}                                                                       
00008000$SUBPROGRAM$                                                            
00009000PROGRAM CONFIG;                                                         
00010000                                                                        
00011000 TYPE                                                                   
00012000     adbc_rec = PACKED RECORD                                           
00013000             code : Integer;                                            0
0014000             lnum : Shortint;                                           
00015000            nhits : Integer;                                           
00016000            chits : Shortint;                                          
00017000            sue   : Integer;                                            
00018000            comp  : Packed Array [1..40] of Char;                       
00019000           serial : Integer;                                            
00020000                                                                       
00021000    END;                                                               
00022000                                                                        
00023000 VAR                                                                    
00024000     output              : text;                                        
00025000     input               : text;                                        
00026000     c_date              : string [80] ;                                
00027000     date_hold           : string [5] ;                                 
00028000     date_hold2          : string [3] ;                                 
00029000     date_comp           : string [6] ;                                 
00029100     file_no             : shortint;                                    
00030000     file_name           : string [26];                                 
00031000     foptions            : Shortint ;                                   
00032000     aoptions            : Shortint ;                                   
00033000     cc                  : Shortint ;                                   
00034000     record_size         : Shortint ;                                   
00035000     in_record           : String [120];                                
00036000     company_name        : String [40];                                 
00037000     adbc_buffer         : adbc_rec;                                    
00038000     var_name            : String [8];                                  
00039000     var_status          : Shortint;                                    
00040000     var_key             : Integer;                                     
00041000     var_value           : Integer;                                     
00042000     new_susan           : Integer;                                     
00043000                                                                        
00044000 PROCEDURE dateline            ; INTRINSIC ;                            
00045000 FUNCTION fopen:shortint       ; INTRINSIC ;                            
00046000 PROCEDURE fread               ; INTRINSIC ;                            
00047000 PROCEDURE fclose              ; INTRINSIC ;                            
00048000 PROCEDURE hpcigetvar          ; INTRINSIC ;                            
00049000 PROCEDURE printfileinfo       ; INTRINSIC ;                            
00050000                                                                        
00051000 PROCEDURE file_system_about;                                           
00052000 BEGIN                                                                  
00053000                                                                        
00054000  writeln(output, 'ADBC Driver file system error [ADBCABOUT1]');        
00055000  printfileinfo(file_no);                                               
00056000  halt(0);                                                              
00057000  END;                                                                  
00058000                                                                        
00059000  PROCEDURE no_access_abort;                                            
00060000  BEGIN                                                                 
00061000        writeln(output,'***** ADBC Driver ABORT *****');                
00062000        writeln(output,'This driver is NOT authorized to run');
00063000        write(output,' on this server.  ');                             
00064000        writeln(output,'Please contact ANSI!');                         
00066000        writeln(output, '(908) 237-1700');                              
00067000        halt(0);                                                        
00068000                                                                        
00069000  END;                                                                  
00070000$CHECK_FORMAL_PARM 0$                                                   
00070100PROCEDURE about;                                                        
00071000BEGIN                                                                   
00072000  rewrite(output,'$stdlist');                                           
00073000                                                                        
00074000  c_date := strrpt (' ', 80);{ fille with blanks}                       
00075000  file_name := strrpt (' ',26);                                         
00076000  in_record := strrpt (' ',120);                                        
00077000                                                                        
00078000  dateline ( c_date ) ;                                                 
00079000  c_date := strrtrim ( c_date ) ;                                       
00080000  date_hold := str(c_date,14,4);                                        
00081000  date_hold2 := str(c_date,6,3);                                        
00082000  writeln(output,'+----------------------------------------+');         
00083000  writeln(output,'* ADBC IMAGE/SQL Driver 2.1.0 * (c) 1997 *');         
00084000  writeln(output,'*       Advanced Network Systems, Inc    *');         
00085000  writeln(output,'*         (908) 638-3330                 *');         
00085200  writeln(output,'+----------------------------------------+');         
00086000  date_hold := strltrim(date_hold);                                     
00088000                                                                        
00088100   IF date_hold2 = 'JAN' THEN                                           
00089000       date_hold2 := '01'                                               
00090000   ELSE IF date_hold2 = 'FEB' THEN                                      
00091000       date_hold2 := '02'                                               
00092000   ELSE IF date_hold2 = 'MAR' THEN                                      
00093000       date_hold2 := '03'                                               
00094000   ELSE IF date_hold2 = 'APR' THEN                                      
00095000       date_hold2 := '04'                                               
00096000   ELSE IF date_hold2 = 'MAY' THEN                                     
00097000       date_hold2 := '05'                                              
00098000   ELSE IF date_hold2 = 'JUN' THEN                                     
00099000       date_hold2 := '06'                                              
00100000   ELSE IF date_hold2 = 'JUL' THEN                                     
00101000       date_hold2 := '07'                                              
00102000   ELSE IF date_hold2 = 'AUG' THEN                                     
00103001       date_hold2 := '08'                                              
00103002   ELSE IF date_hold2 = 'SEP' THEN                                     
00103003       date_hold2 := '09'                                              
00103004   ELSE IF date_hold2 = 'OCT' THEN                                     
00103005       date_hold2 := '10'                                              
00103006   ELSE IF date_hold2 = 'NOV' THEN                                     
00103007       date_hold2 := '11'                                              
00103008   ELSE IF date_hold2 = 'DEC' THEN                                     
00103009       date_hold2 := '12';                                             
00103010   date_comp := date_hold + date_hold2;                                
00103100   date_comp := strltrim(date_comp);                                   
00104000                                                                       
00104100   IF date_comp > '200106' THEN                                        
00105000      BEGIN                                                            
00106000       prompt(output,'Support has expired!! Please call ANSI!');       
00107000       halt(0);




