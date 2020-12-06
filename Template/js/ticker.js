/* Copyright (C) 2020 Christoph Theis */

/*
 
 #caption    [Time][Table][Event][Group][Round]
 #team       [Team A - TeamX][ResA : ResX]
 #match
 [Player A] [G1][G2]... [ResA] 
 -----------------------------
 [Player X] [G1][G2]... [ResX]
 
 */

/* global config */

var ticker = {};

ticker.update = function(name) {
    var timeout = config.liveticker.timeout;

    $.ajax({
        url: name + '.js',
        ifModified: $('div#ticker-content table tbody').length > 0,  // force fill empty ticker
        cache: false,
        dataType: 'json',
        success: function(data) {
            try {
                if (data == null)
                    return;

                for (var i = 0; i < data.length; i++) {
                    ticker.updateData(JSON.parse(data[i]));
                }
            } catch (err) {

            }

        },
        statusCode: {
            500: function() {
                timeout = 90;
            }
        },
        complete: function() {
            if (!config.liveticker.noUpdate && $('div#ticker-content').length > 0)
                setTimeout(function() {ticker.update(name);}, timeout * 1000);
        }
    });
};

ticker.updateData = function(data) {
    if (data.mtTable == 0)
        return;

    var tr = ticker.findRow(data.mtTable);

    tr.html(ticker.formatRow(data));
    
    tr.find('tr.match').click(function(event) {
        event.preventDefault();
        
        displayGames(this);
    });            
};

ticker.findRow = function(id) {
    // Lookup by id
    var row = $('div#ticker-content table.ticker tbody#' + id);
    if (row != null && row.length > 0)
        return row;

    // Not found, insert
    var rows = $('div#ticker-content table.ticker tbody');
    for (var i = 0; i < rows.length; i++) {
        var rowID = $(rows[i]).attr('id');
        if (parseInt(rowID) > id) {
            $('div#ticker-content table.ticker tbody#' + rowID).before('<tbody id="' + id + '"></tbody>');

            return $('div#ticker-content table.ticker tbody#' + id);
        }
    }

    $('div#ticker-content table.ticker').append($('<tbody id="' + id + '"></tbody>'));
    row = $('div#ticker-content table.ticker tbody#' + id);
    
    return row;
};

ticker.formatRow = function(data) {
    // If this is a team match, write teams and team result in first line and individual result in second line
    var hasTeam = data.tmA != null && data.tmA.tmName != "" || data.tmX != null && data.tmX.tmName != "";

    var serviceLeft = data.serviceLeft && data.gameRunning ? ' service' : '';
    var serviceRight = data.serviceRight && data.gameRunning ? ' service' : '';

    var caption = '';
    caption += '<tr class="header">';
    caption += '<th colspan="15">';
    caption += '<span class="headerline">';
    caption += '<span class="event">' + data.cpName + '</span>';
    caption += '<span class="group">' + data.grDesc + '</span>';
    caption += '</span>';
    caption += '<span class="headerline">';
    caption += '<span class="round">' + data.mtRoundString + '</span>';
    caption += '<span class="time">' + formatTime(data.mtDateTime) + '</span>';
    caption += '<span class="mttable">' + 'T.&nbsp;' + data.mtTable + '</span>';
    caption += '</span>';
    caption += '</th>';
    caption += '</tr>';

    var team = '';

    if (hasTeam) {
        team += '<tr class="team">';
        team += '<td colspan="15">';
        team += '<span class="names">';
        team += '<span class="left">' + ticker.formatTeam(data.tmA) + '</span>';
        team += '<span class="center">&nbsp;-&nbsp;</span>';
        team += '<span class="right">' + ticker.formatTeam(data.tmX) + '</span>';
        team += '</span>';
        team += '<span class="result">';
        if (data.tmA != null && data.tmX != null) {
            team += data.tmA.mtRes + '&nbsp;-&nbsp;' + data.tmX.mtRes;
        }
        team += '</span>';
        team += '</td>';
        team += '</tr>';
    }

    var detail = '<tr class="match">';
    
    detail += '<td class="name">';
    detail += '<div class="left">';
    detail += '<span class="playerservice ' + serviceLeft + '"><div></div></span>';
    detail += ticker.formatPlayers(data.plA, data.plB, hasTeam, data.tmA);
    detail += '</div>'; // left
    detail += '<div class="right">';
    detail += '<span class="playerservice ' + serviceRight + '"><div></div></span>';
    detail += ticker.formatPlayers(data.plX, data.plY, hasTeam, data.tmX);
    detail += '</div>'; // right
    detail += '</td>';
    
    detail += '<td class="result">';
    detail += '<div class="left">' + data.mtResA + '</div>';
    detail += '<div class="right">' + data.mtResX + '</div>';
    detail += '</td>';

    detail += ticker.formatGames(data);
    
    return caption + team + detail;

};


ticker.formatTeam = function(tm) {
    if (tm === null || tm.tmDesc === '')
        return '&nbsp;';
    
    return tm.tmDesc;
};

ticker.formatPlayers = function(plA, plB, isTeam, tm) {
    if (plA == null || plA.plNr == 0) {
        var s = '<span class="player">';
        
        if (isTeam && tm !== undefined && tm !== null && tm.tmDesc !== undefined){
            if (config.flagtype === 1)
                s += '<span class="flag"><img src="flags/' + tm.naName + '.png"></span>';    
            else if (config.flagtype === 2)
                s += '<span class="flag"><img src="flags/' + tm.naRegion + '.png"></span>';    
            else
                s += '<span class="flag"></span>';
            
            s += '<span class="name">' + tm.tmDesc + '</span>';
        }
        s += '</span>';
        
        return s;
    }

    var s = '<span class="player">';
    if (config.flagtype === 1)
        s += '<span class="flag"><img src="flags/' + plA.naName + '.png"></span>';    
    else if (config.flagtype === 2)
        s += '<span class="flag"><img src="flags/' + plA.naRegion + '.png"></span>';    
    else
        s += '<span class="flag"></span>';
    s += '<span class="name">' + plA.psLast + ',&nbsp;' + plA.psFirst + '</span>';
    if (!isTeam)
        s += '<span class="assoc">' + plA.naName + '</span>';
    s += '</span>';
    
    if (plB !== undefined && plB !== null && plB.plNr > 0) {
        s += '<span class="player">';
        if (config.flagtype === 1)
            s += '<span class="flag"><img src="flags/' + plB.naName + '.png"></span>';    
        else if (config.flagtype === 2)
            s += '<span class="flag"><img src="flags/' + plB.naRegion + '.png"></span>';    
        else
            s += '<span class="flag"></span>';
        s += '<span class="name">' + plB.psLast + ',&nbsp;' + plB.psFirst + '</span>';
        if (!isTeam)
            s += '<span class="assoc">' + plB.naName + '</span>';
        s += '</span>';
    }

    return s;
};

ticker.formatGames = function(data) {
    var s = '';
    var i = 0;

    for (i = 0; i < 7; i++) {
        if (data.mtSets == null || i == data.mtSets.length || !data.matchRunning) {
            if (i == 0) 
                s += '<td class="game current"></td>';

            break;
        }

        if (data.mtSets[i][0] == 0 && data.mtSets[i][1] == 0) {  
            if (2 * data.mtResA < data.mtBestOf && 2 * data.mtResX < data.mtBestOf) 
                s += '<td class="game current"></td>';
            break;
        }

        s += '<td class="game ';
        if ( i == 6 || i == data.mtSets.length - 1 || 
             data.mtSets[i+1][0] == 0 && data.mtSets[i+1][1] == 0 ) {
         s += 'current';
        }
        
        s += '">';
        s += '<div class="left">' + data.mtSets[i][0] + '</div>';
        s += '<div class="right">' + data.mtSets[i][1] + '</div>';
        s += '</td>';
    }

    if (i < 6)
        s += '<td class="game" colspan="' + (7 - i) + '"></td>';

    return s;
};




