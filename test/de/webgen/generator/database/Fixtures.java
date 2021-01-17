/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator.database;

public class Fixtures {
    static String[] players = {
        "{plID: 1, plNr: 1001, psFirstName: Faa, psLastName: Laa, psSex: 1, naName: NAA, naDesc: Daa, naRegion: RAA}",
        "{plID: 2, plNr: 1002, psFirstName: Fbb, psLastName: Lbb, psSex: 1, naName: NBB, naDesc: Dbb, naRegion: RBB}",
        "{plID: 3, plNr: 1003, psFirstName: Fcc, psLastName: Lcc, psSex: 1, naName: NCC, naDesc: Dcc, naRegion: RCC}",
        "{plID: 4, plNr: 1004, psFirstName: Fdd, psLastName: Ldd, psSex: 1, naName: NDD, naDesc: Ddd, naRegion: RDD}"
    };
    
    static String[] competitions = {
        "{cpID: 1, cpName: TEST, cpDesc: \"Men's Singles\", cpCategory: Men, cpType: 1}"
    };
    
    static String[] groups = {
        "{cpID: 1, grID: 1, grName: RR, grDesc: \"Knock Out\", grModus: 1, grSize: 4}",
        "{cpID: 1, grID: 2, grName: KO, grDesc: \"Knock Out\", grModus: 2, grSize: 4}",
        "{cpID: 1, grID: 4, grName: PKO, grDesc: \"Progressive Knock Out\", grModus: 2, grSize: 4}"
    };
    
    static String[] singles = {
        // RR 4 players
        "{grID: 1, mtID: 1, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-01T09:00\", mtTable: 1, plAplID: 1, plXplID: 3, stA: 1, stX: 3, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 2, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-01T10:00\", mtTable: 1, plAplID: 2, plXplID: 4, stA: 2, stX: 4, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 3, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-01T11:00\", mtTable: 1, plAplID: 1, plXplID: 2, stA: 1, stX: 2, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 4, mtNr: 4, mtMS: 0, mtRound: 2, mtMatch: 2, mtDateTime: \"2020-12-01T12:00\", mtTable: 1, plAplID: 3, plXplID: 4, stA: 3, stX: 4, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 5, mtNr: 5, mtMS: 0, mtRound: 3, mtMatch: 1, mtDateTime: \"2020-12-01T13:00\", mtTable: 1, plAplID: 1, plXplID: 4, stA: 1, stX: 4, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 6, mtNr: 6, mtMS: 0, mtRound: 3, mtMatch: 2, mtDateTime: \"2020-12-01T14:00\", mtTable: 1, plAplID: 2, plXplID: 3, stA: 2, stX: 3, mtResA: 0, mtResX: 0}",
        // KO 4 players
        "{grID: 2, mtID: 7, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-02T09:00\", mtTable: 1, plAplID: 1, plXplID: 2, mtResA: 0, mtResX: 0}",
        "{grID: 2, mtID: 8, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-02T10:00\", mtTable: 1, plAplID: 3, plXplID: 4, mtResA: 0, mtResX: 0}",
        "{grID: 2, mtID: 9, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-02T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
        // PLO 4 players
        "{grID: 4, mtID: 10, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-04T09:00\", mtTable: 1, plAplID: 1, plXplID: 2, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 11, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-04T10:00\", mtTable: 1, plAplID: 3, plXplID: 4, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 12, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-04T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 13, mtNr: 4, mtMS: 0, mtRound: 2, mtMatch: 2, mtDateTime: \"2020-12-04T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
    };
}
