       IDENTIFICATION DIVISION.
       PROGRAM-ID. SEMANTIC-TARGET.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-X PIC X(8).
       01 FLAG PIC 9.
       01 AUX-PGM PIC X(8).
       PROCEDURE DIVISION.
           MOVE 'A' TO WS-X.
           MOVE 'AUXPGM' TO AUX-PGM.
           IF FLAG = 1
               MOVE 'B' TO WS-X
               IF FLAG = 2
                   CALL AUX-PGM
               ELSE
                   MOVE 'NEST' TO AUX-PGM
               END-IF
               MOVE 'AFTER' TO AUX-PGM
           ELSE
               MOVE 'C' TO WS-X
           END-IF.
           CALL WS-X.
           CALL AUX-PGM.
           CALL 'STATIC-PGM'.
       END PROGRAM SEMANTIC-TARGET.
