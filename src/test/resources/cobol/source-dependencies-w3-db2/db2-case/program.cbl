       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2PGM.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-SQL PIC X(80).
       PROCEDURE DIVISION.
       EXEC SQL
       select * from cliente
       END-EXEC.
       EXEC SQL
       Select * From CLIENTE
       END-EXEC.
       GOBACK.
