/* Copyright (C) 2020 Christoph Theis */

var translations = {
    'de' : {
        'date' : {
            'MMMM' : {
                '1' : 'Januar',
                '2' : 'Februar',
                '3' : 'März',
                '4' : 'April',
                '5' : 'Mai',
                '6' : 'Juni',
                '7' : 'Juli',
                '8' : 'August',
                '9' : 'September',
                '10' : 'Oktober',
                '11' : 'November',
                '12' : 'Dezember'
            },
            'MMM' : {
                '1' : 'Jan',
                '2' : 'Feb',
                '3' : 'Mär',
                '4' : 'Apr',
                '5' : 'Mai',
                '6' : 'Jun',
                '7' : 'Jul',
                '8' : 'Aug',
                '9' : 'Sep',
                '10' : 'Okt',
                '11' : 'Nov',
                '12' : 'Dez'
            },
            'EEEE' : {
                '1' : 'Sonntag',
                '2' : 'Montag',
                '3' : 'Dienstag',
                '4' : 'Mitttwoch',
                '5' : 'Donnerstag',
                '6' : 'Freitag',
                '7' : 'Samstag'
            },
            'EEE' : {
                '1' : 'So',
                '2' : 'Mo',
                '3' : 'Di',
                '4' : 'Mi',
                '5' : 'Do',
                '6' : 'Fr',
                '7' : 'Sa'
            }
        },
        'nav' : {
            'show-nav' : 'Nav. Ein',
            'hide-nav' : 'Nav. Aus',
            'show-dates' : 'Auswahl Datum',
            'hide-dates' : 'Auswahl Datum',
            'show-groups' : 'Auswahl Gruppen',
            'hide-groups' : 'Auswahl Gruppen',
            'show-reports' : 'Auswahl Listen',
            'hide-reports' : 'Auswahl Listen',
            'draw' : 'Auslosungen',
            'matches' : 'Spiele',
            'players' : 'Spieler',
            'liveticker' : 'Live Ticker',
            'news' : 'News',
            'ko' : {
                'one-round' : 'Eine Runde',
                'all-rounds' : 'Alle Runden',
                'last-32' : 'Letzte 32',
                'teammatches' : 'Mannschaftsspiele'
            },
            'report' : {
                'teams' : 'Mannschaften',
                'players' : 'Spieler'
            }
        },
        'events' : {
            'filter' : {
                'groups' : 'Gruppen:'
            }
        },
        'rr' : {
            'title' : '__cpdesc__ - __grdesc__',
            'mt-pts' : 'Mt. Pkt.',
            'games' : 'Sätze',
            'stdng' : 'Pos.',
            'format' : {
                'long' : {
                    'date' : '$t(date.EEEE.__wday__), __day__ $t(date.MMMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : '__round__. Runde'
                },
                'short' : {
                    'round' : 'Rd. __round__'
                }
            }
        },
        'ko' : {
            'title' : '__cpdesc__ - __grdesc__',
            'winner' : 'Sieger',
            'first' : 'Erste',
            'prev' : 'Vorige',
            'next' : 'Nächste',
            'last' : 'Letzte',
            'format' : {
                'long' : {
                    'round' : '__round__. Runde',
                    'qualification' : 'Qualifikation'
                },
                'short' : {
                    'date' : '$t(date.EEE.__wday__), __day__ $t(date.MMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : 'Rd. __round__',
                    'roundof' : '1/__matches__',
                    'qualification' : 'Qu.',
                    'final' : 'F',
                    'semifinal' : 'SF'
                }
            }
        },
        'pko' : {
            'format' : {
                'short' : {
                    'pos' : 'Pos. __from__ - __to__'                    
                }
            }
        },
        'result' : {
            'format' : {
                'short' : {
                    'wo' : 'w/o',
                    'injured' : 'Verl.',
                    'disqualified' : 'Disqu.'
                }
            }
        },
        'report' : {
            'name' : {
                'players' : 'Teilnehmer',
                'singles' : 'Teilnehmer im Einzel',
                'doubles' : 'Teilnehmer im Doppel',
                'mixed'   : 'Teilnehmer in Mixed',
                'teams'   : 'Teilnehmer in Mannschaft',
                'partnerwanted' : 'Spieler ohne Partner'
            },
            'filter' : {
                'filter' : 'Filter',
                'events' : 'Wettbewerbe: ',
                'assocs' : 'Verbände: ',
                'names' : 'Name: ',
                'startno' : 'Startnr.: ',
                'tables' : 'Tische: ',
                'all-events' : 'Alle Wettbewerbe',
                'all-assocs' : 'Alle Verbände'
            },
            'format' : {
                'long' : {                     
                    'date' : '$t(date.EEE.__wday__), __day__ $t(date.MMM.__month__)'
                },
                'short' : {
                    'date' : '__day__ $t(date.MMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : 'Rd. __round__',
                    'roundof' : '1/__matches__',
                    'qualification' : 'Qu.',
                    'final' : 'F',
                    'semifinal' : 'SF'
                }
            },
            'plnr' : 'St. Nr.',
            'plname' : 'Name',
            'pssex' : 'Geschlecht',
            'assoc' : 'Verband',
            'event' : 'Wettbewerb',
            'team'  : 'Mannschaft'
        },
        'dates' : {
            'filter' : {
                'filter' : 'Filter',
                'events' : 'Wettbewerbe: ',
                'assocs' : 'Verbände: ',
                'names' : 'Name: ',
                'all-events' : 'Alle Wettbewerbe',
                'all-assocs' : 'Alle Verbände'
            },
            'date' : {
                'format' : '$t(date.EEE.__wday__), $t(date.MMMM.__month__) __day__'            
            },
            'time' : {
                'no-time' : 'Ohne Zeit'

            },
            'title' : 'Spiele von $t(date.EEEE.__wday__), __day__. $t(date.MMMM.__month__)'
        }
    },
    'en' : {
        'date' : {
            'MMMM' : {
                '1' : 'January',
                '2' : 'February',
                '3' : 'March',
                '4' : 'April',
                '5' : 'May',
                '6' : 'June',
                '7' : 'July',
                '8' : 'August',
                '9' : 'September',
                '10' : 'October',
                '11' : 'November',
                '12' : 'December'
            },
            'MMM' : {
                '1' : 'Jan',
                '2' : 'Feb',
                '3' : 'Mar',
                '4' : 'Apr',
                '5' : 'May',
                '6' : 'Jun',
                '7' : 'Jul',
                '8' : 'Aug',
                '9' : 'Sep',
                '10' : 'Oct',
                '11' : 'Nov',
                '12' : 'Dec'
            },
            'EEEE' : {
                '1' : 'Sunday',
                '2' : 'Monday',
                '3' : 'Tuesday',
                '4' : 'Wednesday',
                '5' : 'Thursday',
                '6' : 'Friday',
                '7' : 'Saturday'
            },
            'EEE' : {
                '1' : 'Sun',
                '2' : 'Mon',
                '3' : 'Tue',
                '4' : 'Wed',
                '5' : 'Thu',
                '6' : 'Fri',
                '7' : 'Sat'
            }
        },
        'nav' : {
            'show-nav' : 'Show Nav',
            'hide-nav' : 'Hide Nav',
            'show-dates' : 'Select Date',
            'hide-dates' : 'Select Date',
            'show-groups' : 'Select Group',
            'hide-groups' : 'Select Group',
            'show-reports' : 'Select List',
            'hide-reports' : 'Select List',
            'draw' : 'Draws',
            'matches' : 'Matches',
            'players' : 'Players',
            'liveticker' : 'Live Ticker',
            'news' : 'News',
            'ko' : {
                'one-round' : 'One Round',
                'all-rounds' : 'All Rounds',
                'last-32' : 'Last 32',
                'teammatches' : 'Team Matches'
            },
            'report' : {
                'teams' : 'Teams',
                'players' : 'Players'
            }
        },
        'events' : {
            'filter' : {
                'groups' : 'Groups:'
            }
        },
        'rr' : {
            'title' : '__cpdesc__ - __grdesc__',
            'mt-pts' : 'Mt. Pts.',
            'games' : 'Games',
            'stdng' : 'Stdng.',
            'format' : {
                'long' : {
                    'date' : '$t(date.EEEE.__wday__), __day__ $t(date.MMMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : '__round__. Round'
                },
                'short' : {
                    'round' : 'Rd. __round__'
                }
            }
        },
        'ko' : {
            'title' : '__cpdesc__ - __grdesc__',
            'first' : 'First',
            'prev' : 'Prev',
            'next' : 'Next',
            'last' : 'Last',
            'winner' : 'Winner',
            'format' : {
                'long' : {
                    'round' : '__round__. Round',                    
                    'qualification' : 'Qualification'
                },
                'short' : {
                    'date' : '$t(date.EEE.__wday__), __day__ $t(date.MMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : 'Rd. __round__',
                    'roundof' : 'Rd. of __entries__',
                    'qualification' : 'Qu.',
                    'final' : 'F',
                    'semifinal' : 'SF'
                }
            }
        },
        'pko' : {
            'format' : {
                'short' : {
                    'pos' : 'Pos. __from__ - __to__'                    
                }
            }
        },
        'result' : {
            'format' : {
                'short' : {
                    'wo' : 'w/o',
                    'injured' : 'Inj.',
                    'disqualified' : 'Disqu.'
                }
            }
        },
        'report' : {
            'name' : {
                'players' : 'Participants',
                'singles' : 'Participants in Singles',
                'doubles' : 'Participants in Doubles',
                'mixed'   : 'Participants in Mixed',
                'teams'   : 'Participants in Teams',
                'partnerwanted' : 'Participants with Partner Missing'
            },
            'filter' : {
                'filter' : 'Filter',
                'events' : 'Events: ',
                'assocs' : 'Associations: ',
                'names' : 'Name: ',
                'startno' : 'Start No: ',
                'tables' : 'Tables: ',
                'all-events' : 'All Events',
                'all-assocs' : 'All Associations'
            },
            'format' : {
                'long' : {                     
                    'date' : '$t(date.EEE.__wday__), __day__ $t(date.MMM.__month__)'
                },
                'short' : {
                    'date' : '__day__ $t(date.MMM.__month__)',
                    'time' : '__hour__:__minute__',
                    'table' : 'T. __table__',
                    'round' : 'Rd. __round__',
                    'roundof' : 'Rd. of __entries__',
                    'qualification' : 'Qu.',
                    'final' : 'F',
                    'semifinal' : 'SF'
                }
            },
            'plnr' : 'Pl. No.',
            'plname' : 'Name',
            'pssex' : 'Sex',
            'assoc' : 'Association',
            'event' : 'Event',
            'team'  : 'Team'
        },
        'dates' : {
            'filter' : {
                'filter' : 'Filter',
                'events' : 'Events: ',
                'assocs' : 'Associations: ',
                'names' : 'Name: ',
                'all-events' : 'All Events',
                'all-assocs' : 'All Associations'
            },
            'date' : {
                'format' : '$t(date.EEE.__wday__), $t(date.MMMM.__month__) __day__'            
            },
            'time' : {
                'no-time' : 'No Time'

            },
            'title' : 'Matches from $t(date.EEEE.__wday__), $t(date.MMMM.__month__) __day__'
        }
    }
};
