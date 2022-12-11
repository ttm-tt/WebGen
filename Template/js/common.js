/* Copyright (C) 2020 Christoph Theis */

function getParameterByName(name, def, href)
{
    if (href === undefined)
    var params = new URLSearchParams(window.location.search);
    if (!params.has(name))
        return def;
    
    return params.get(name);
}

function formatString(s, len) {
    if (s == undefined)
        return '';

    return s.substring(0, len);
}


function formatTime(time) {
    // iOS could not parse an iso date time, so cut out what we want
    if ( typeof time == 'string')
        return time.slice(11, 16);
    else if (typeof time != 'number')
        return '';
    
    var d = new Date(time);
    var hours = d.getHours();
    var minutes = d.getMinutes();

    var s = '';
    if (hours < 10)
        s += '0';
    s += hours;
    s += ':';
    if (minutes < 10)
        s += '0';
    s += minutes;

    return s;
}


function formatDate(date) {
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate();
    
    return cts;
}


function formatDateTime(date) {
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate() + ' ' + 
        (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + 
        (date.getMinutes() < 10 ? '0' : '') + date.getMinutes() + ':' + 
        (date.getSeconds() < 10 ? '0' : '') + date.getSeconds() + '.' + 
        (date.getMilliseconds() < 10 ? '00' : (date.getMilliseconds < 100 ? '0' : '')) + date.getMilliseconds();

    return cts;
}

function formatISODateTime(date) {
    if (typeof date != 'object')
        return formatISODateTime(new Date(date));
    
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate() + 'T' + 
        (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + 
        (date.getMinutes() < 10 ? '0' : '') + date.getMinutes() + ':' + 
        (date.getSeconds() < 10 ? '0' : '') + date.getSeconds() + '.' + 
        (date.getMilliseconds() < 10 ? '00' : (date.getMilliseconds < 100 ? '0' : '')) + date.getMilliseconds();

    return cts;
}

function formatISODate(date) {
    if (typeof date != 'object')
        return formatISODate(new Date(date));
    
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate();

    return cts;
}

function beep(duration) {
    var ctx = new(window.audioContext || window.webkitAudioContext);
    var type = 0;  // 0: sinus, 1: rechteck, 2: saegezahn, 3: dreieck

    duration = +duration;

    // Only 0-4 are valid types.
    // type = (type % 5) || 0;

    var osc = ctx.createOscillator();

    osc.type = type;

    osc.connect(ctx.destination);
    osc.noteOn(0);

    setTimeout(function () {
        osc.noteOff(0);
    }, duration);

}

