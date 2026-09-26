# Real F2 source / AST / SP diagnostic dossier

Extracted source is evidence; expected edges are independently reasoned in D0_F2_DEEP_DIVE.md.

## CBACT01C

### statement:89 / statement:158
Source: {'endColumn': 14, 'endLine': 333, 'file': 'CBACT01C.cbl', 'startColumn': 11, 'startLine': 333}; caller: PERFORM 0000-ACCTFILE-OPEN
AST control: `{"id": 743, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 405, "startColumn": 4, "endLine": 405, "endColumn": 7}, "original": {"file": "CBACT01C.cbl", "startLine": 333, "startColumn": 11, "endLine": 333, "endColumn": 14}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 746, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  400         DISPLAY 'ERROR OPENING ACCTFILE'
  401         MOVE ACCTFILE-STATUS TO IO-STATUS
  402         PERFORM 9910-DISPLAY-IO-STATUS
  403         PERFORM 9999-ABEND-PROGRAM
  404     END-IF
  405     EXIT.
  406 2000-OUTFILE-OPEN.
  407     MOVE 8 TO APPL-RESULT.
  408     OPEN OUTPUT OUT-FILE
  409     IF   OUTFILE-STATUS = '00'
  410         MOVE 0 TO APPL-RESULT
```

## CBACT02C

### statement:31 / statement:49
Source: {'endColumn': 14, 'endLine': 134, 'file': 'CBACT02C.cbl', 'startColumn': 11, 'startLine': 134}; caller: PERFORM 0000-CARDFILE-OPEN
AST control: `{"id": 185, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 148, "startColumn": 4, "endLine": 148, "endColumn": 7}, "original": {"file": "CBACT02C.cbl", "startLine": 134, "startColumn": 11, "endLine": 134, "endColumn": 14}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 188, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  143         DISPLAY 'ERROR OPENING CARDFILE'
  144         MOVE CARDFILE-STATUS TO IO-STATUS
  145         PERFORM 9910-DISPLAY-IO-STATUS
  146         PERFORM 9999-ABEND-PROGRAM
  147     END-IF
  148     EXIT.
  149 *> ---------------------------------------------------------------*
  150 9000-CARDFILE-CLOSE.
  151     ADD 8 TO ZERO GIVING APPL-RESULT.
  152     CLOSE CARDFILE-FILE
  153     IF  CARDFILE-STATUS = '00'
```

## COACTUPC

### statement:858 / statement:1284
Source: {'endColumn': 41, 'endLine': 23, 'file': 'CSSTRPFY.cpy', 'startColumn': 15, 'startLine': 23}; caller: PERFORM YYYY-STORE-PFKEY THRU YYYY-STORE-PFKEY-EXIT
AST control: `{"id": 9727, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 7608, "startColumn": 8, "endLine": 7608, "endColumn": 34}, "original": {"file": "CSSTRPFY.cpy", "startLine": 23, "startColumn": 15, "endLine": 23, "endColumn": 41}, "includeChain": [{"includingFile": "COACTUPC.cbl", "requestedName": "CSSTRPFY", "includedFile": "CSSTRPFY.cpy", "includeLine": 4199}], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 9977, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 7603 *> ****************************************************************
 7604 *>  Map AID to PFKey in COMMON Area
 7605 *> ****************************************************************
 7606     EVALUATE TRUE
 7607       WHEN EIBAID IS EQUAL TO DFHENTER
 7608         SET CCARD-AID-ENTER TO TRUE
 7609       WHEN EIBAID IS EQUAL TO DFHCLEAR
 7610         SET CCARD-AID-CLEAR TO TRUE
 7611       WHEN EIBAID IS EQUAL TO DFHPA1
 7612         SET CCARD-AID-PA1  TO TRUE
 7613       WHEN EIBAID IS EQUAL TO DFHPA2
```

### statement:858 / statement:1312
Source: {'endColumn': 14, 'endLine': 81, 'file': 'CSSTRPFY.cpy', 'startColumn': 11, 'startLine': 81}; caller: PERFORM YYYY-STORE-PFKEY THRU YYYY-STORE-PFKEY-EXIT
AST control: `{"id": 9977, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 7666, "startColumn": 4, "endLine": 7666, "endColumn": 7}, "original": {"file": "CSSTRPFY.cpy", "startLine": 81, "startColumn": 11, "endLine": 81, "endColumn": 14}, "includeChain": [{"includingFile": "COACTUPC.cbl", "requestedName": "CSSTRPFY", "includedFile": "CSSTRPFY.cpy", "includeLine": 4199}], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 9980, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 7661       WHEN EIBAID IS EQUAL TO DFHPF24
 7662         SET CCARD-AID-PFK12 TO TRUE
 7663     END-EVALUATE
 7664     .
 7665 YYYY-STORE-PFKEY-EXIT.
 7666     EXIT
 7667     .
 7668 *>
 7669 *>  Ver: CardDemo_v1.0-15-g27d6c6f-68 Date: 2022-07-19 23:15:59 CDT
 7670 *>
 7671
```

### statement:886 / statement:943
Source: {'endColumn': 21, 'endLine': 1672, 'file': 'COACTUPC.cbl', 'startColumn': 14, 'startLine': 1672}; caller: PERFORM 1200-EDIT-MAP-INPUTS THRU 1200-EDIT-MAP-INPUTS-EXIT
AST control: `{"id": 4778, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 4058, "startColumn": 7, "endLine": 4058, "endColumn": 14}, "original": {"file": "COACTUPC.cbl", "startLine": 1672, "startColumn": 14, "endLine": 1672, "endColumn": 21}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 4786, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 4053         PERFORM 1280-EDIT-US-STATE-ZIP-CD
 4054            THRU 1280-EDIT-US-STATE-ZIP-CD-EXIT
 4055     END-IF
 4056
 4057     IF INPUT-ERROR
 4058        CONTINUE
 4059     ELSE
 4060        SET ACUP-CHANGES-OK-NOT-CONFIRMED TO TRUE
 4061     END-IF
 4062     .
 4063
```

### statement:916 / statement:1317
Source: {'endColumn': 47, 'endLine': 19, 'file': 'CSUTLDPY.cpy', 'startColumn': 11, 'startLine': 19}; caller: PERFORM EDIT-DATE-CCYYMMDD THRU EDIT-DATE-CCYYMMDD-EXIT
AST control: `{"id": 9998, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 7715, "startColumn": 4, "endLine": 7715, "endColumn": 40}, "original": {"file": "CSUTLDPY.cpy", "startLine": 19, "startColumn": 11, "endLine": 19, "endColumn": 47}, "includeChain": [{"includingFile": "COACTUPC.cbl", "requestedName": "CSUTLDPY", "includedFile": "CSUTLDPY.cpy", "includeLine": 4232}], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 10005, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 7710 *>       d) EDIT-DATE-OF-BIRTH
 7711 *>       e) EDIT-DATE-OF-BIRTH
 7712 *> *****************************************************************
 7713
 7714 EDIT-DATE-CCYYMMDD.
 7715     SET WS-EDIT-DATE-IS-INVALID   TO TRUE
 7716     .
 7717
 7718 *> *****************************************************************
 7719 *> Check for valid year and century
 7720 *> *****************************************************************
```

### statement:1210 / statement:1222
Source: {'endColumn': 29, 'endLine': 3684, 'file': 'COACTUPC.cbl', 'startColumn': 20, 'startLine': 3674}; caller: PERFORM 9200-GETCARDXREF-BYACCT THRU 9200-GETCARDXREF-BYACCT-EXIT
AST control: `{"id": 8855, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 7100, "startColumn": 13, "endLine": 7110, "endColumn": 22}, "original": {"file": "COACTUPC.cbl", "startLine": 3674, "startColumn": 20, "endLine": 3684, "endColumn": 29}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 8900, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 7095            SET INPUT-ERROR                 TO TRUE
 7096            SET FLG-ACCTFILTER-NOT-OK       TO TRUE
 7097            IF WS-RETURN-MSG-OFF
 7098              MOVE WS-RESP-CD               TO ERROR-RESP
 7099              MOVE WS-REAS-CD               TO ERROR-RESP2
 7100              STRING
 7101              'Account:'
 7102               WS-CARD-RID-ACCT-ID-X
 7103              ' not found in'
 7104              ' Cross ref file.  Resp:'
 7105              ERROR-RESP
 7106              ' Reas:'
 7107              ERROR-RESP2
 7108              DELIMITED BY SIZE
 7109              INTO WS-RETURN-MSG
 7110              END-STRING
 7111            END-IF
 7112         WHEN OTHER
 7113            SET INPUT-ERROR                 TO TRUE
 7114            SET FLG-ACCTFILTER-NOT-OK                TO TRUE
 7115            MOVE 'READ'                     TO ERROR-OPNAME
```

## COCRDUPC

### statement:306 / statement:344
Source: {'endColumn': 46, 'endLine': 839, 'file': 'COCRDUPC.cbl', 'startColumn': 11, 'startLine': 839}; caller: PERFORM 1230-EDIT-NAME THRU 1230-EDIT-NAME-EXIT
AST control: `{"id": 2008, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 1357, "startColumn": 4, "endLine": 1357, "endColumn": 39}, "original": {"file": "COCRDUPC.cbl", "startLine": 839, "startColumn": 11, "endLine": 839, "endColumn": 46}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 2015, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 1352           SET WS-NAME-MUST-BE-ALPHA  TO TRUE
 1353        END-IF
 1354        GO TO  1230-EDIT-NAME-EXIT
 1355     END-IF
 1356
 1357     SET FLG-CARDNAME-ISVALID     TO TRUE
 1358     .
 1359 1230-EDIT-NAME-EXIT.
 1360     EXIT
 1361     .
 1362
```

## CBEXPORT

### statement:94 / statement:106
Source: {'endColumn': 58, 'endLine': 169, 'file': 'CBEXPORT.cbl', 'startColumn': 11, 'startLine': 169}; caller: PERFORM 1000-INITIALIZE
AST control: `{"id": 429, "type": "PreservedStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 364, "startColumn": 4, "endLine": 364, "endColumn": 51}, "original": {"file": "CBEXPORT.cbl", "startLine": 169, "startColumn": 11, "endLine": 169, "endColumn": 58}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 436, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  359
  360     PERFORM 1050-GENERATE-TIMESTAMP
  361     PERFORM 1100-OPEN-FILES
  362
  363     DISPLAY 'CBEXPORT: Export Date: ' WS-EXPORT-DATE
  364     DISPLAY 'CBEXPORT: Export Time: ' WS-EXPORT-TIME.
  365
  366 *> ****************************************************************
  367 1050-GENERATE-TIMESTAMP.
  368 *> ****************************************************************
  369 *>     Get current date and time
```

### statement:103 / statement:111
Source: {'endColumn': 20, 'endLine': 194, 'file': 'CBEXPORT.cbl', 'startColumn': 11, 'startLine': 191}; caller: PERFORM 1050-GENERATE-TIMESTAMP
AST control: `{"id": 468, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 386, "startColumn": 4, "endLine": 389, "endColumn": 13}, "original": {"file": "CBEXPORT.cbl", "startLine": 191, "startColumn": 11, "endLine": 194, "endColumn": 20}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 481, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  381         DELIMITED BY SIZE
  382         INTO WS-EXPORT-TIME
  383     END-STRING
  384
  385 *>     Create 26-character timestamp for export records
  386     STRING WS-EXPORT-DATE ' ' WS-EXPORT-TIME '.00'
  387         DELIMITED BY SIZE
  388         INTO WS-FORMATTED-TIMESTAMP
  389     END-STRING
  390     .
  391
  392 *> ****************************************************************
  393 1100-OPEN-FILES.
  394 *> ****************************************************************
```

### statement:133 / statement:145
Source: {'endColumn': 44, 'endLine': 310, 'file': 'CBEXPORT.cbl', 'startColumn': 11, 'startLine': 310}; caller: PERFORM 2200-CREATE-CUSTOMER-EXP-REC
AST control: `{"id": 704, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 505, "startColumn": 4, "endLine": 505, "endColumn": 37}, "original": {"file": "CBEXPORT.cbl", "startLine": 310, "startColumn": 11, "endLine": 310, "endColumn": 44}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 711, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RESUME_NOT_PROVEN', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE', 'CONTAINMENT_NOT_PROJECTED']
```cobol
  500                 WS-EXPORT-STATUS
  501         PERFORM 9999-ABEND-PROGRAM
  502     END-IF
  503
  504     ADD 1 TO WS-CUSTOMER-RECORDS-EXPORTED
  505     ADD 1 TO WS-TOTAL-RECORDS-EXPORTED.
  506 *> ************************************************************
  507 3000-EXPORT-ACCOUNTS.
  508 *> ****************************************************************
  509     DISPLAY 'CBEXPORT: Processing account records'
  510
```

## COACCT01

### statement:136 / statement:158
Source: {'endColumn': 48, 'endLine': 314, 'file': 'COACCT01.cbl', 'startColumn': 20, 'startLine': 314}; caller: PERFORM 2100-OPEN-ERROR-QUEUE
AST control: `{"id": 534, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 337, "startColumn": 13, "endLine": 337, "endColumn": 41}, "original": {"file": "COACCT01.cbl", "startLine": 314, "startColumn": 20, "endLine": 314, "endColumn": 48}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 559, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  332     EVALUATE MQ-CONDITION-CODE
  333         WHEN MQCC-OK
  334              MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
  335              MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
  336              MOVE MQ-HOBJ           TO ERROR-QUEUE-HANDLE
  337              SET  ERR-QUEUE-OPEN   TO TRUE
  338         WHEN OTHER
  339              MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
  340              MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
  341              MOVE ERROR-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
  342              MOVE 'ERR MQOPEN ERR'  TO MQ-APPL-RETURN-MESSAGE
```

### statement:143 / statement:166
Source: {'endColumn': 51, 'endLine': 375, 'file': 'COACCT01.cbl', 'startColumn': 14, 'startLine': 375}; caller: PERFORM 3000-GET-REQUEST
AST control: `{"id": 660, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 397, "startColumn": 7, "endLine": 397, "endColumn": 44}, "original": {"file": "COACCT01.cbl", "startLine": 375, "startColumn": 14, "endLine": 375, "endColumn": 51}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 692, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  392        MOVE MQ-CORRELID       TO SAVE-CORELID
  393        MOVE MQ-QUEUE-REPLY    TO SAVE-REPLY2Q
  394        MOVE MQ-MSG-ID         TO SAVE-MSGID
  395        MOVE REQUEST-MESSAGE   TO REQUEST-MSG-COPY
  396        PERFORM 4000-PROCESS-REQUEST-REPLY
  397        ADD  1                 TO MQ-MSG-COUNT
  398     ELSE
  399        IF MQ-REASON-CODE  =  MQRC-NO-MSG-AVAILABLE
  400          SET NO-MORE-MSGS             TO  TRUE
  401
  402        ELSE
```

## COPAUS0C

### statement:271 / statement:291
Source: {'endColumn': 49, 'endLine': 383, 'file': 'COPAUS0C.cbl', 'startColumn': 14, 'startLine': 383}; caller: PERFORM PROCESS-PF7-KEY
AST control: `{"id": 2626, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 1493, "startColumn": 7, "endLine": 1493, "endColumn": 42}, "original": {"file": "COPAUS0C.cbl", "startLine": 383, "startColumn": 14, "endLine": 383, "endColumn": 49}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 2633, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE', 'CONTAINMENT_NOT_PROJECTED']
```cobol
 1488
 1489        PERFORM PROCESS-PAGE-FORWARD
 1490     ELSE
 1491        MOVE 'You are already at the top of the page...' TO
 1492                         WS-MESSAGE
 1493        SET SEND-ERASE-NO            TO TRUE
 1494     END-IF
 1495     .
 1496
 1497 *> ****************************************************************
 1498 PROCESS-PF8-KEY.
```

### statement:300 / statement:319
Source: {'endColumn': 26, 'endLine': 604, 'file': 'COPAUS0C.cbl', 'startColumn': 19, 'startLine': 604}; caller: PERFORM POPULATE-AUTH-LIST
AST control: `{"id": 3138, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 1714, "startColumn": 12, "endLine": 1714, "endColumn": 19}, "original": {"file": "COPAUS0C.cbl", "startLine": 604, "startColumn": 19, "endLine": 604, "endColumn": 26}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 3141, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RESUME_NOT_PROVEN', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
 1709             MOVE WS-AUTH-APRV-STAT TO PAPRV05I OF COPAU0AI
 1710             MOVE PA-MATCH-STATUS   TO PSTAT05I OF COPAU0AI
 1711             MOVE WS-AUTH-AMT       TO PAMT005I OF COPAU0AI
 1712             MOVE DFHBMUNP          TO SEL0005A OF COPAU0AI
 1713         WHEN OTHER
 1714             CONTINUE
 1715     END-EVALUATE.
 1716
 1717 *> ****************************************************************
 1718 INITIALIZE-AUTH-DATA.
 1719 *> ****************************************************************
```

## COPAUS1C

### statement:131 / statement:168
Source: {'endColumn': 58, 'endLine': 474, 'file': 'COPAUS1C.cbl', 'startColumn': 21, 'startLine': 474}; caller: PERFORM READ-AUTH-RECORD
AST control: `{"id": 1766, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 1077, "startColumn": 14, "endLine": 1077, "endColumn": 51}, "original": {"file": "COPAUS1C.cbl", "startLine": 474, "startColumn": 21, "endLine": 474, "endColumn": 58}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 1794, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
 1072        END-EXEC}*>ENDDLI
 1073
 1074        MOVE DIBSTAT                          TO IMS-RETURN-CODE
 1075        EVALUATE TRUE
 1076            WHEN STATUS-OK
 1077               SET AUTHS-NOT-EOF              TO TRUE
 1078            WHEN SEGMENT-NOT-FOUND
 1079            WHEN END-OF-DB
 1080               SET AUTHS-EOF                  TO TRUE
 1081            WHEN OTHER
 1082               MOVE 'Y'     TO WS-ERR-FLG
```

## COBIL00C

### statement:127 / statement:152
Source: {'endColumn': 26, 'endLine': 358, 'file': 'COBIL00C.cbl', 'startColumn': 19, 'startLine': 358}; caller: PERFORM READ-ACCTDAT-FILE
AST control: `{"id": 1020, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 684, "startColumn": 12, "endLine": 684, "endColumn": 19}, "original": {"file": "COBIL00C.cbl", "startLine": 358, "startColumn": 19, "endLine": 358, "endColumn": 26}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 1063, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE', 'CONTAINMENT_NOT_PROJECTED']
```cobol
  679     *>EXECCICS EXEC CICS READ          DATASET   (WS-ACCTDAT-FILE)          INTO      (ACCOUNT-RECORD)          LENGTH    (LENGTH OF ACCOUNT-RECORD)          RIDFLD    (ACCT-ID)          KEYLENGTH (LENGTH OF ACCT-ID)          UPDATE          RESP      (WS-RESP-CD)          RESP2     (WS-REAS-CD)     END-EXEC
  680
  681
  682     EVALUATE WS-RESP-CD
  683         WHEN DFHRESP(NORMAL)
  684             CONTINUE
  685         WHEN DFHRESP(NOTFND)
  686             MOVE 'Y'     TO WS-ERR-FLG
  687             MOVE 'Account ID NOT found...' TO
  688                             WS-MESSAGE
  689             MOVE -1       TO ACTIDINL OF COBIL0AI
```

## COTRTUPC

### statement:157 / statement:196
Source: {'endColumn': 51, 'endLine': 602, 'file': 'COTRTUPC.cbl', 'startColumn': 14, 'startLine': 602}; caller: PERFORM 0001-CHECK-PFKEYS THRU 0001-CHECK-PFKEYS-EXIT
AST control: `{"id": 1470, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 1150, "startColumn": 7, "endLine": 1150, "endColumn": 44}, "original": {"file": "COTRTUPC.cbl", "startLine": 602, "startColumn": 14, "endLine": 602, "endColumn": 51}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 1489, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ORDINARY_INCOMING_NOT_EXCLUDED']
```cobol
 1145                        OR   TTUP-DETAILS-NOT-FOUND
 1146                        OR   TTUP-CONFIRM-DELETE
 1147                        OR   TTUP-CREATE-NEW-RECORD
 1148                             )
 1149       )
 1150        SET PFK-VALID                  TO TRUE
 1151     ELSE
 1152        SET PFK-INVALID                TO TRUE
 1153        IF WS-RETURN-MSG-OFF
 1154           SET WS-INVALID-KEY-PRESSED  TO TRUE
 1155        END-IF
```

## COPAUA0C

### statement:185 / statement:192
Source: {'endColumn': 14, 'endLine': 250, 'file': 'COPAUA0C.cbl', 'startColumn': 11, 'startLine': 250}; caller: PERFORM 1000-INITIALIZE    THRU 1000-EXIT
AST control: `{"id": 585, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 499, "startColumn": 4, "endLine": 499, "endColumn": 7}, "original": {"file": "COPAUA0C.cbl", "startLine": 250, "startColumn": 11, "endLine": 250, "endColumn": 14}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 588, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN']
```cobol
  494
  495     PERFORM 3100-READ-REQUEST-MQ    THRU 3100-EXIT
  496     .
  497 *>
  498 1000-EXIT.
  499     EXIT.
  500 *>
  501 *>  ------------------------------------------------------------- *
  502 *>   OPEN THE REQUEST QUEUE                                       *
  503 *>  ------------------------------------------_------------------ *
  504 1100-OPEN-REQUEST-QUEUE.
```

### statement:229 / statement:240
Source: {'endColumn': 47, 'endLine': 489, 'file': 'COPAUA0C.cbl', 'startColumn': 19, 'startLine': 489}; caller: PERFORM 5100-READ-XREF-RECORD     THRU 5100-EXIT
AST control: `{"id": 981, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 730, "startColumn": 12, "endLine": 730, "endColumn": 40}, "original": {"file": "COPAUA0C.cbl", "startLine": 489, "startColumn": 19, "endLine": 489, "endColumn": 47}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 1057, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN']
```cobol
  725     *>EXECCICS EXEC CICS READ                                                         DATASET   (WS-CCXREF-FILE)                                        INTO      (CARD-XREF-RECORD)                                      LENGTH    (LENGTH OF CARD-XREF-RECORD)                            RIDFLD    (XREF-CARD-NUM)                                         KEYLENGTH (LENGTH OF XREF-CARD-NUM)                               RESP      (WS-RESP-CD)                                            RESP2     (WS-REAS-CD)                                       END-EXEC
  726
  727
  728     EVALUATE WS-RESP-CD
  729         WHEN DFHRESP(NORMAL)
  730             SET  CARD-FOUND-XREF  TO TRUE
  731         WHEN DFHRESP(NOTFND)
  732             SET  CARD-NFOUND-XREF TO TRUE
  733             SET  NFOUND-ACCT-IN-MSTR TO TRUE
  734
  735             MOVE 'A001'          TO ERR-LOCATION
```

### statement:234 / statement:292
Source: {'endColumn': 20, 'endLine': 731, 'file': 'COPAUA0C.cbl', 'startColumn': 11, 'startLine': 722}; caller: PERFORM 6000-MAKE-DECISION        THRU 6000-EXIT
AST control: `{"id": 1429, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 949, "startColumn": 4, "endLine": 958, "endColumn": 13}, "original": {"file": "COPAUA0C.cbl", "startLine": 722, "startColumn": 11, "endLine": 731, "endColumn": 20}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 1460, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_INPUT_INCOMPLETE', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN']
```cobol
  944        END-EVALUATE
  945     END-IF
  946
  947     MOVE WS-APPROVED-AMT        TO WS-APPROVED-AMT-DIS
  948
  949     STRING PA-RL-CARD-NUM         ','
  950            PA-RL-TRANSACTION-ID   ','
  951            PA-RL-AUTH-ID-CODE     ','
  952            PA-RL-AUTH-RESP-CODE   ','
  953            PA-RL-AUTH-RESP-REASON ','
  954            WS-APPROVED-AMT-DIS    ','
  955            DELIMITED BY SIZE
  956            INTO W02-PUT-BUFFER
  957            WITH POINTER WS-RESP-LENGTH
  958     END-STRING
  959     .
  960 *>
  961 6000-EXIT.
  962     EXIT.
  963 *>
```

## CBTRN02C

### statement:271 / statement:277
Source: {'endColumn': 22, 'endLine': 518, 'file': 'CBTRN02C.cbl', 'startColumn': 15, 'startLine': 518}; caller: PERFORM 2700-A-CREATE-TCATBAL-REC
AST control: `{"id": 937, "type": "ModeledStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 604, "startColumn": 8, "endLine": 604, "endColumn": 15}, "original": {"file": "CBTRN02C.cbl", "startLine": 518, "startColumn": 15, "endLine": 518, "endColumn": 22}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 950, "normalCompletionRecognized": true}`
SP completion membership: True; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  599         MOVE 0 TO APPL-RESULT
  600     ELSE
  601         MOVE 12 TO APPL-RESULT
  602     END-IF
  603     IF  APPL-AOK
  604         CONTINUE
  605     ELSE
  606         DISPLAY 'ERROR WRITING TRANSACTION BALANCE FILE'
  607         MOVE TCATBALF-STATUS TO IO-STATUS
  608         PERFORM 9910-DISPLAY-IO-STATUS
  609         PERFORM 9999-ABEND-PROGRAM
```

## CBTRN01C

### statement:107 / statement:130
Source: {'endColumn': 53, 'endLine': 238, 'file': 'CBTRN01C.cbl', 'startColumn': 18, 'startLine': 238}; caller: PERFORM 2000-LOOKUP-XREF
AST control: `{"id": 463, "type": "PreservedStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 351, "startColumn": 11, "endLine": 351, "endColumn": 46}, "original": {"file": "CBTRN01C.cbl", "startLine": 238, "startColumn": 18, "endLine": 238, "endColumn": 53}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 470, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_RESUME_NOT_PROVEN', 'PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN']
```cobol
  346            MOVE 4 TO WS-XREF-READ-STATUS
  347          NOT INVALID KEY
  348            DISPLAY 'SUCCESSFUL READ OF XREF'
  349            DISPLAY 'CARD NUMBER: ' XREF-CARD-NUM
  350            DISPLAY 'ACCOUNT ID : ' XREF-ACCT-ID
  351            DISPLAY 'CUSTOMER ID: ' XREF-CUST-ID
  352     END-READ.
  353 *> ---------------------------------------------------------------*
  354 3000-READ-ACCOUNT.
  355     MOVE ACCT-ID TO FD-ACCT-ID
  356     READ ACCOUNT-FILE RECORD INTO ACCOUNT-RECORD
```

## CBTRN03C

### statement:143 / statement:169
Source: {'endColumn': 34, 'endLine': 233, 'file': 'CBTRN03C.cbl', 'startColumn': 14, 'startLine': 232}; caller: PERFORM 0550-DATEPARM-READ
AST control: `{"id": 499, "type": "PreservedStatement", "source": {"expanded": {"file": "<preprocessed>", "startLine": 359, "startColumn": 7, "endLine": 360, "endColumn": 27}, "original": {"file": "CBTRN03C.cbl", "startLine": 232, "startColumn": 14, "endLine": 233, "endColumn": 34}, "includeChain": [], "exact": true}, "paragraphLocalNext": null, "ordinaryNext": 525, "normalCompletionRecognized": true}`
SP completion membership: False; gaps: ['PERFORM_RANGE_CONTROL_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_NOT_PROVEN', 'PERFORM_ISOLATED_PRIMARY_FLOW_NOT_PROVEN', 'PERFORM_LINEAR_MOVE_BODY_NOT_PROVEN', 'PERFORM_PROVENANCE_INCOMPLETE']
```cobol
  354       WHEN OTHER
  355           MOVE 12 TO APPL-RESULT
  356     END-EVALUATE
  357
  358     IF APPL-AOK
  359        DISPLAY 'Reporting from ' WS-START-DATE
  360           ' to ' WS-END-DATE
  361     ELSE
  362        IF APPL-EOF
  363           MOVE 'Y' TO END-OF-FILE
  364        ELSE
  365           DISPLAY 'ERROR READING DATEPARM FILE'
```
