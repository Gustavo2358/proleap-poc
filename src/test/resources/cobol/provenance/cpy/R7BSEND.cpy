           EXEC CICS SEND
               FROM(WS-AREA)
               LENGTH(LENGTH OF WS-AREA)
               NOHANDLE ERASE
           END-EXEC.
