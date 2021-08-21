/* Copyright (C) 2020 Christoph Theis */

/* global config, filePath, translations, ticker, i18next, jqueryI18next, i18nextBrowserLanguageDetector */

var webgen = {};

var isEttu = (typeof filePath) !== 'undefined';
var refreshSpan = '<span class="oi oi-reload"></span>';
var refreshLoad = (isEttu ? '<img src="/?proxy=img/ajax-loader.gif"></img>' : '<img src="img/ajax-loader.gif"></img>');


function refresh() {
    window.location.hash = '';

    if (localStorage !== undefined && localStorage !== null) {
        localStorage.setItem('webgen', JSON.stringify(webgen));

        location.reload();
    } else {
        restoreData();
    }
}


function restoreData() {
    if (localStorage !== undefined && localStorage !== null && localStorage.getItem('webgen') !== null) {
        webgen = JSON.parse(localStorage.getItem('webgen'));

        localStorage.removeItem('webgen');

        if (webgen.lastNav === null) {
            webgen.lastNav = undefined;
            webgen.lastContent = undefined;
            webgen.lastFilter = {};
        }

        if (webgen.lastContent === null) {
            webgen.lastContent = undefined;
            webgen.lastFilter = {};
        }
    }

    if (webgen.lastNav !== undefined) {
        if ($.find('div.navbar-collapse #' + webgen.lastNav).length === 0) {
            webgen.lastNav = undefined;
            webgen.lastContent = undefined;
            webgen.lastFilter = {};
        }
    }

    if (webgen.lastFilter === undefined)
        webgen.lastFilter = {};

    if (window.location.hash !== undefined &&
            window.location.hash !== null &&
            window.location.hash.length > 1) {
        webgen.lastNav = window.location.hash.substr(1);  // skip leading '#'
        webgen.lastContent = undefined;
        webgen.lastFilter = {};
    } else if (window.location.search !== undefined &&
            window.location.search !== null &&
            window.location.search.length > 1) {
        webgen.lastNav = getParameterByName('nav', 'dates');
        webgen.lastContent = getParameterByName('content', undefined);
        if (webgen.lastContent !== undefined)
            webgen.lastContent += '.html';
        webgen.lastFilter = {};

        var filterEvent = getParameterByName('filterEvent', undefined);
        var filterAssoc = getParameterByName('filterAssoc', undefined);
        var filterExtId = getParameterByName('filterExtId', undefined);
        var filterStartNo = getParameterByName('filterStartNo', undefined);

        if (filterEvent !== undefined) {
            webgen.lastFilter[webgen.lastNav] = {};
            webgen.lastFilter[webgen.lastNav].event = filterEvent;
        }

        if (filterAssoc !== undefined) {
            webgen.lastFilter[webgen.lastNav] = {};
            webgen.lastFilter[webgen.lastNav].assoc = filterAssoc;
        }

        if (filterStartNo !== undefined) {
            webgen.lastFilter[webgen.lastNav] = {};
            webgen.lastFilter[webgen.lastNav].startno = filterStartNo;
        }

        if (filterExtId !== undefined) {
            webgen.lastFilter[webgen.lastNav] = {};
            webgen.lastFilter[webgen.lastNav].extid = filterExtId;
        }
    }

    // Verfiy validity of lastNav
    if (webgen.lastNav !== undefined && $('#navbar li a#' + webgen.lastNav).length === 0)
        webgen.lastNav = undefined;

    if (webgen.lastNav === undefined) {
        webgen.lastContent = undefined;
        webgen.lastFilter = {};

        if (config.liveticker)
            webgen.lastNav = 'liveticker';
        else {
            webgen.lastNav = 'dates';
        }
    }

    if (webgen.lastNav !== undefined)
        loadNav(webgen.lastNav);
}


$(document).ready(function () {
    // Load ETTU specific scripts, if we think we are ETTU
    if (isEttu) {
        $('head').append(
                '<link href="css/ettu.css" rel="stylesheet">'
                );

        $('head').append(
                '<script type="text/javascript" src="js/ettu.js"></script>'
                );
    }

    // Load site specific configs
    if (config.config !== '') {
        $('head').append(
                '<link href="css/' + config.config + '.css" rel="stylesheet">'
                );

        $('head').append(
                '<script type="text/javascript" src="js/' + config.config + '.js"></script>'
                );
    }

    // And finally user scripts
    $('head').append(
            '<link href="css/user.css" rel="stylesheet">'
            );

    $('head').append(
            '<script type="text/javascript" src="js/user.js"></script>'
            );

    // Set title, init Liveticker
    if (config.description !== '')
        document.title = config.description.replace(/&rsquo;/g, "'");

    $('div.navbar a#title').html(config.title);
    if (config.url)
        $('div.navbar a#title').html('<a href="' + config.url + '" target="_blank">' + config.title + '</a>');

    if (!config.liveticker)
        $('div.navbar a#liveticker').parent().hide();

    if (!config.news)
        $('div.navbar a#news').parent().hide();
    else
        $('div.navbar a#news').attr('data-href', config.news);

    // Hide "Show Nav" button initially
    $('button#toggle-nav').hide();

    // Stop Ajax from using cache
    $.ajaxSetup({
        cache: false
    });

    // Initialize I18N
    i18next
            .use(i18nextBrowserLanguageDetector)
            .init(
            {
                fallbackLng: 'en',
                resources: translations
            },
            function () {
                jqueryI18next.init(i18next, $, {useOptionsAttr: true});
                $(document).localize();
            }
    );

    // -------------------------------------------------------------------
    // Hide / Show sub navigation on small devices
    $('[data-bs-toggle=offcanvas]').click(function () {
        // $('#sidebar').toggleClass('hidden-xs');
        $('.row-offcanvas').toggleClass('active');
        if ($('.row-offcanvas').hasClass('active'))
            $(this).text(i18next.t('nav.hide-' + webgen.lastNav));
        else
            $(this).text(i18next.t('nav.show-' + webgen.lastNav));
    });


    // -------------------------------------------------------------------
    // Navigate main menu
    $('#navbar .nav-link').click(function (event) {
        event.preventDefault();

        // Clear sidebar and content
        $('div#sidebar').html('');
        $('div#content').html('');

        $('button#toggle-nav').show();

        var id = this.id;

        loadNav(id);

        // Should be outside of loadNav, because loadNav is also called from refresh
        if (!$('.row-offcanvas').hasClass('active'))
            $('[data-bs-toggle=offcanvas]').click();
    });

    // Close popover when clicking outside
    $('html').on('click', function (e) {
        // Condition: neither this element (which could be a <span> inside an <a>),
        // nor any of the parents is a popover
        // And non of the parents has the .popover class (so we are outside the popover)
        // Elements with popover are recognized by data-original-title, which is
        // set by BS
        if ($(e.target).closest('[data-original-title]').length === 0 &&
                !$(e.target).parents().is('.popover.show')) {
            $('[data-original-title]').popover('hide');
        }
    });

    window.matchMedia('print').addListener(function (mql) {
        if (mql.matches)
            $('#content table.report tbody:visible tr[data-href]').trigger('show.bs.collapse');
    });

    restoreData();
});


function loadNav(id) {
    if (webgen.lastNav !== id) {
        webgen.lastNav = id;
        webgen.lastContent = undefined;
    }

    var href = (isEttu ? folderPath(filePath) + id + '.html' : id + '.html');
    var target = $('#' + id).attr('data-bs-target') || '#sidebar';

    $(target).css('visibility', 'hidden');

    if (id === 'liveticker')
        $('button#offcanvas-toggle-button').parent().hide();
    else
        $('button#offcanvas-toggle-button').parent().show();

    $('button#offcanvas-toggle-button').text(i18next.t('nav.hide-' + webgen.lastNav));

    $(target).load(href, function () {
        $(target).localize();

        $('.navbar-collapse').collapse('hide');

        if (typeof (window[id]) === 'function')
            window[id]();

        initCollapsible($(this));

        $(target).css('visibility', 'visible');

        // Special cases
        if (id === 'dates') {
            // Dates starts with current date, if it exists
            var ct = formatDate(new Date());
            var link = $('a[href="' + ct + '.html' + '"]');
            if (link !== undefined && link.length > 0) {
                dates.load(link.attr('href'));
                
                // Hide nav if shown
                if ($('.row-offcanvas').hasClass('active'))
                    $('[data-bs-toggle=offcanvas]').click();
            } else {
                if (!$('.row-offcanvas').hasClass('active'))
                    $('[data-bs-toggle=offcanvas]').click();
            }
        } else if (id === 'reports') {
            // Reports starts with players, if it exists
            var link = $('a[href="players.html"');
            if (link !== undefined && link.length > 0) {
                reports.load(link.attr('href'));
                
                // Hide nav if shown
                if ($('.row-offcanvas').hasClass('active'))
                    $('[data-bs-toggle=offcanvas]').click();
            } else {
                if (!$('.row-offcanvas').hasClass('active'))
                    $('[data-bs-toggle=offcanvas]').click();
            }            
        }
    });

    // Move .active
    $('#' + id).parent().parent().find('.active').removeClass('active');
    $('#' + id).parent().addClass('active');
}


function initCollapsible(which) {
    // -------------------------------------------------------------------
    // Add chevron up / down to accorion
    $(which).find('.collapse').on('show.bs.collapse', function () {
        $(this).parent().find('[data-bs-toggle="collapse"] span.oi')
                .removeClass('oi-chevron-left')
                .addClass('oi-chevron-bottom');
    });

    $(which).find('.collapse').on('hide.bs.collapse', function () {
        $(this).parent().find('[data-bs-toggle="collapse"] span.oi')
                .removeClass('oi-chevron-bottom')
                .addClass('oi-chevron-left');
    });
}
;

// Display detailed results
function displayGames(match) {
    if ($(match).find('td.name').is(':hidden') || $(match).find('td.game').is(':hidden')) {
        if ($(match).hasClass('alternate'))
            $(match).removeClass('alternate');
        else
            $(match).addClass('alternate');
    }
}

// -----------------------------------------------------------------------
// Navigate sub nav
this.events = function () {
    // Change the href for the pdf
    if (isEttu) {
        $('.panel-title a.pdf').each(function () {
            this.href = folderPath(filePath) + fileBasename(this.href);
        });
    }

    // Add click handler to reports menu
    $('#sidebar li.list-group-item span.cpname a').click(function (event) {
        event.preventDefault();

        var href = (isEttu ? folderPath(filePath) + fileBasename(this.href) : this.href);
        events.load(href);

        if ($('.row-offcanvas').hasClass('active'))
            $('[data-bs-toggle=offcanvas]').click();
    });

    events.load = function (href) {
        if (webgen.lastContent !== href) {
            webgen.lastContent = href;
        }

        $('#content').load(href, function () {
            $('#content').localize();

            var select = $('#content #filter select#groups');
            if (select.find('option').length <= 1)
                $('div#filter').hide();
            else
                $('div#filter').show();

            select.on('change', function () {
                events.changeGroup();
            });

            initCollapsible($(this));

            // And load first group
            events.changeGroup();
        });
    };

    events.changeGroup = function () {
        var item = $('#content #filter select#groups :selected');
        var href = item.attr('data-webgen-href');

        if (webgen.lastFilter !== href) {
            webgen.lastFilter = href;
        }

        $('button.refresh').html(refreshLoad);

        $('#group-content').load(href + '.html', function () {
            // Reparse entire content, not only group-content, because we may change the title
            $('#content').localize();

            // When flags are to be shown replace all src attributes with the data-href- one
            if (config.flagtype > 0) {
                $('#content span.flag img').each(function () {
                    var href = config.flagtype == 1 ? $(this).data('webgen-nation') : $(this).data('webgen-region');
                    if (href !== undefined) {
                        $(this).attr('src', 'flags/' + href + '.png');
                        $(this).attr('alt', href);
                    }
                });
            }

            // Navigation between 1 Round, All Rounds, Last 32 (if KO)
            $('div#ko-nav a:first').addClass('active');

            $('div#ko-nav a').click(function (event) {
                event.preventDefault();

                // Move .active
                $(this).parent().parent().find('.active').removeClass('active');
                $(this).addClass('active');

                var id = this.id;
                $('div.ko-content').hide();
                $('div#' + id + '-content').show();

                $('div#ko-nav li').removeClass('active');
                $(this).parent().addClass('active');
            });

            $('div#ko-nav li a').first().click();

            $('div.ko-content div#ko-nav-rounds a').click(function (event) {
                event.preventDefault();

                var id = this.id;
                var current = $('div.ko-content .item.active');

                $(current).removeClass('active');

                if (id === 'ko-first') {
                    $('div.ko-content').each(function () {
                        $(this).find('.item').first().addClass('active');
                    });
                } else if (id === 'ko-last')
                    $('div.ko-content').each(function () {
                        $(this).find('.item').last().addClass('active');
                    });
                else if (id === 'ko-prev' && $(current).prev('.item').length > 0)
                    $(current).prev('.item').addClass('active');
                else if (id === 'ko-next' && $(current).next('.item').length > 0)
                    $(current).next('.item').addClass('active');
                else
                    $(current).addClass('active');

                current = $('div.ko-content .item.active');
                if ($(current).prev('.item').length > 0) {
                    $('div#ko-nav-rounds .page-item.ko-first').removeClass('disabled');
                    $('div#ko-nav-rounds .page-item.ko-prev').removeClass('disabled');
                } else {
                    $('div#ko-nav-rounds .page-item.ko-first').addClass('disabled');
                    $('div#ko-nav-rounds .page-item.ko-prev').addClass('disabled');
                }
                if ($(current).next('.item').length > 0) {
                    $('div#ko-nav-rounds .page-item.ko-last').removeClass('disabled');
                    $('div#ko-nav-rounds .page-item.ko-next').removeClass('disabled');
                } else {
                    $('div#ko-nav-rounds .page-item.ko-last').addClass('disabled');
                    $('div#ko-nav-rounds .page-item.ko-next').addClass('disabled');
                }
            });

            $('div#ko-nav-rounds .page-item.ko-first a').click();

            // Add handler for detailed result
            $('table.matches tr.match').not('[data-bs-toggle=collapse]').click(function (event) {
                event.preventDefault();

                displayGames(this);
            });

            $('table.matches tr.individual').click(function (event) {
                event.preventDefault();

                displayGames(this);
            });

            $('[data-webgen-stid]').hover(function () {
                var stid = $(this).data('webgen-stid');
                $('[data-webgen-stid="' + stid + '"]').addClass('kopath');
            }, function () {
                var stid = $(this).data('webgen-stid');
                $('[data-webgen-stid="' + stid + '"]').removeClass('kopath');
            });

            $('[data-bs-toggle="popover"]')
                    .popover({
                        html: true,
                        sanitize: false, // We trust our own content
                        trigger: 'click',
                        // container: 'body',
                        placement: 'bottom',
                        // fallbackPlacement: 'bottom',
                        content: function () {
                            var target = $(this).data('bs-target');
                            var html = $('tbody' + target).html();
                            var classes = $('tbody' + target).parent().hasClass('rr') ? 'rr' : 'ko';

                            return '<div class="' + classes + '"><table class="' + classes + ' matches">' + html + '</table></div>';
                        }
                    })
                    .click(function () {
                        // $(this).popover('show');
                    })
                    .on('shown.bs.popover', function () {
                        $('.popover-body table').find('.collapse').addClass('show');

                        $('.popover table.matches tr.individual').click(function (event) {
                            event.preventDefault();

                            displayGames(this);
                        });
                    })
                    ;

            $('button.refresh').html(refreshSpan);
        });
    };

    if (webgen.lastNav === 'events' && webgen.lastContent !== undefined)
        events.load(webgen.lastContent);
};


this.dates = function () {
    // Filter table
    function filterMatches(event, assoc, name, tables) {
        if (tables === undefined)
            tables = [];

        webgen.lastFilter.dates = {event: event, assoc: assoc, name: name, tables: tables};

        // An empty string plitted is an array with one element which is an empty string
        if (tables.length == 1 && tables[0] == '')
            tables = [];

        $('#times tbody').each(function () {
            var eventMatch = (event === undefined) || (event === '');
            var assocMatch = (assoc === undefined) || (assoc === '');
            var nameMatch = (name === undefined) || (name === '');
            var tableMatch = (tables === undefined) || (tables.length === 0);

            if (!eventMatch)
                eventMatch |= $(this).find('tr.header span.event').text() === event;
            if (!assocMatch)
                $(this).find('tr.match span.assoc').each(function () {
                    assocMatch |= $(this).text() === assoc;
                });
            if (!nameMatch)
                $(this).find('tr.match span.name').each(function () {
                    nameMatch |= $(this).text().toLowerCase().indexOf(name) === 0;
                });

            var t = $(this).find('tr.header span.mttable').data("i18n-options");
            if (t !== undefined && !tableMatch)
                $.each(tables, function (idx, val) {
                    if (val.length > 0)
                        tableMatch |= t.table === val;
                });

            if (eventMatch && assocMatch && nameMatch && tableMatch)
                $(this).removeClass('filtered');
            else
                $(this).addClass('filtered');
        });

        $('#times div.timeround').hide();
        $('#times div.timeround').has('tbody:not(".filtered")').show();
    }

    // Add click handler to dates menu
    $('#sidebar a.list-group-item').click(function (event) {
        event.preventDefault();

        var href = (isEttu ? folderPath(filePath) + fileBasename(this.href) : this.href);
        dates.load(href);

        if ($('.row-offcanvas').hasClass('active'))
            $('[data-bs-toggle=offcanvas]').click();
    });

    dates.load = function (href) {
        if (webgen.lastContent !== href) {
            webgen.lastContent = href;
        }

        $('button.refresh').html(refreshLoad);

        $('#content').load(href, function () {
            $('#content').localize();

            // When flags are to be shown replace all src attributes with the data-href- one
            if (config.flagtype > 0) {
                $('#content span.flag img').each(function () {
                    var href = config.flagtype == 1 ? $(this).data('webgen-nation') : $(this).data('webgen-region');
                    if (href !== undefined) {
                        $(this).attr('src', 'flags/' + href + '.png');
                        $(this).attr('alt', href);
                    }
                });
            }

            // Add handler for filter
            $('#content .filter select#events').change(function () {
                var event = $(this).val();
                var assoc = $('#content .filter select#assocs').val();
                var name = $('#content .filter input#names').val().toLowerCase();
                var tables = $('#content .filter input#tables').val().split(',');

                filterMatches(event, assoc, name, tables);
            });

            $('#content .filter select#assocs').change(function () {
                var assoc = $(this).val();
                var event = $('#content .filter select#events').val();
                var name = $('#content .filter input#names').val().toLowerCase();
                var tables = $('#content .filter input#tables').val().split(',');

                filterMatches(event, assoc, name, tables);
            });

            var keyDownNameTimerMatches = 0;
            $('#content .filter input#names').keydown(function () {
                clearTimeout(keyDownNameTimerMatches);
                keyDownNameTimerMatches = setTimeout(function () {
                    var event = $('#content .filter select#events').val();
                    var assoc = $('#content .filter select#assocs').val();
                    var name = $('#content .filter input#names').val().toLowerCase();
                    var tables = $('#content .filter input#tables').val().split(',');

                    filterMatches(event, assoc, name, tables);

                }, 250);
            });

            $('#content .filter input#tables').keydown(function () {
                clearTimeout(keyDownNameTimerMatches);
                keyDownNameTimerMatches = setTimeout(function () {
                    var event = $('#content .filter select#events').val();
                    var assoc = $('#content .filter select#assocs').val();
                    var name = $('#content .filter input#names').val().toLowerCase();
                    var tables = $('#content .filter input#tables').val().split(',');

                    filterMatches(event, assoc, name, tables);

                }, 250);
            });

            // Add handler for detailed results to <table> parent
            $('div#times table.matches').click(function (event) {
                var id;

                if (!(id = event.target.closest('tr.match:not([data-bs-toggle=collapse]), tr.individual')))
                    return;

                event.preventDefault;

                displayGames(id);
            });

            initCollapsible($(this));

            if (webgen.lastNav === 'dates' && webgen.lastFilter.dates !== undefined) {
                $('#content div.filter select#events').val(webgen.lastFilter.dates.event);
                $('#content div.filter select#assocs').val(webgen.lastFilter.dates.assoc);
                $('#content div.filter select#name').val(webgen.lastFilter.dates.name);
                $('#content div.filter select#tables').val(webgen.lastFilter.dates.tables);

                filterMatches(
                        webgen.lastFilter.dates.event,
                        webgen.lastFilter.dates.assoc,
                        webgen.lastFilter.dates.name,
                        webgen.lastFilter.dates.tables
                        );
            }

            $('button.refresh').html(refreshSpan);
        });
    };

    if (webgen.lastNav === 'dates' && webgen.lastContent !== undefined)
        dates.load(webgen.lastContent);
};


this.reports = function () {
    function gotoPage(page, div) {
        if (div === undefined) {
            $('div.report-content').each(function () {
                gotoPage(page, this.id);
            });

            return;
        }

        $('div#' + div + ' table.report > tbody').hide();
        $('div#' + div + ' table.report > tbody').not('.filtered').slice((page - 1) * 20, page * 20).show();

        var numPages = Math.floor(($('div#' + div + ' table.report > tbody').not('.filtered').length + 19) / 20);
        var pagination = '<ul class="pagination">';
        if (page == 1)
            pagination += '<li class="page-item disabled"><a class="page-link page-first">&laquo;</a></li>';
        else
            pagination += '<li class="page-item"><a class="page-link page-first">&laquo;</a></li>';

        var startPage = Math.max(page - 4, 1);
        var endPage = Math.min(page + 4, numPages);
        if (startPage > page - 4)
            endPage = Math.min(startPage + 8, numPages);
        if (endPage < page + 4)
            startPage = Math.max(endPage - 8, 1);

        var xxsStartPage = Math.max(page - 2, 1);
        var xxsEndPage = Math.min(page + 2, numPages);
        if (xxsStartPage > page - 2)
            xxsEndPage = Math.min(xxsStartPage + 4, numPages);
        if (xxsEndPage < page + 2)
            xxsStartPage = Math.max(xxsEndPage - 4, 1);

        for (var p = startPage; p <= endPage; p++) {
            if (p == page)
                pagination += '<li class="page-item active"><a class="page-link page-first page-nr">' + p + '</a></li>';
            else if (p < xxsStartPage || p > xxsEndPage)
                pagination += '<li class="page-item d-none d-sm-block"><a class="page-link page-nr">' + p + '</a></li>';
            else
                pagination += '<li clase="page-item"><a class="page-link page-nr">' + p + '</a></li>';
        }
        if (page == numPages)
            pagination += '<li class="page-item disabled"><a class="page-link page-last">&raquo;</a></li>';
        else
            pagination += '<li class="page-item"><a class="page-link page-last">&raquo;</a></li>';

        $('div#' + div + ' #pagination').html(pagination);

        $('div#' + div + ' #pagination a').click(function (event) {
            event.preventDefault();

            if ($(this).hasClass('page-first'))
                gotoPage(1, div);
            else if ($(this).hasClass('page-last'))
                gotoPage(numPages, div);
            else
                gotoPage(parseInt($(this).html()), div);
        });
    }

    function filterEntries(event, assocName, name, startno, extid) {
        webgen.lastFilter.reports = {event: event, assoc: assocName, name: name, startno: startno, extid: extid};

        // Translate assoc to full name
        var assoc = assocName === '' ? '' : $('#assocs option[value="' + assocName + '"]').text();

        $('#report table.report > tbody').each(function () {
            var eventMatch = (event === '') || (event === undefined);
            var assocMatch = (assoc === '') || (assoc === undefined);
            var nameMatch = (name === '') || (name === undefined);
            var startnoMatch = (startno === '') || (startno === undefined);
            var extidMatch = (extid === '') || (extid === undefined);

            if (!eventMatch)
                eventMatch |= $(this).find('tr.report td.event').text() === event;

            if (!assocMatch)
                $(this).find('tr.report td.assoc div').each(function () {
                    assocMatch |= $(this).text() === assoc;
                });

            if (!nameMatch)
                $(this).find('tr.report td.name div').each(function () {
                    nameMatch |= $(this).text().toLowerCase().indexOf(name) === 0;
                });

            if (!startnoMatch)
                $(this).find('tr.report td.plnr div').each(function () {
                    startnoMatch |= $(this).text() === startno;
                });

            if (!extidMatch)
                $(this).find('tr.report td.extid div').each(function () {
                    extidMatch |= $(this).text() === extid;
                });

            if (eventMatch && assocMatch && nameMatch && startnoMatch && extidMatch) {
                $(this).removeClass('filtered');
            } else {
                $(this).addClass('filtered');
            }
        });

        gotoPage(1);
    }

    // Sort by number
    function sortNumeric(a, b) {
        if ($.text([a]) === $.text([b]))
            return 0;

        return parseFloat($.text([a])) > parseFloat($.text([b])) ? +1 : -1;
    }

    // Sort by text
    function sortAlpha(a, b) {
        if ($.text([a]) === $.text([b]))
            return 0;

        return $.text([a]) > $.text([b]) ? +1 : -1;
    }

    // Add click handler to reports menu
    $('#sidebar a.list-group-item').click(function (event) {
        event.preventDefault();

        var href = (isEttu ? folderPath(filePath) + fileBasename(this.href) : this.href);
        reports.load(href);

        if ($('.row-offcanvas').hasClass('active'))
            $('[data-bs-toggle=offcanvas]').click();
    });

    reports.load = function (href) {
        if (webgen.lastContent !== href) {
            webgen.lastContent = href;
        }

        if (webgen.lastFilter.reports === undefined)
            webgen.lastFilter.reports = {};

        $('button.refresh').html(refreshLoad);

        $('#content').load(href, function () {
            $('#content').localize();
            // Needed for iOS? Maybe, maybenot.
            // $('#content').find('.collapse').collapse({toggle: false});

            // set colspan of details to number of visible columns in the header
            $('div.report-content').each(function () {
                var table = $(this).find('table.report');
                var colspan = $(table).find(' > thead th:visible').length;
                $(table).find('> tbody tr.collapse td').attr('colspan', colspan);
            });

            // Navigation between different types of reports (i.e. Teams and Players in teams)
            $('div#report-nav a').click(function (event) {
                event.preventDefault();

                // Move .active
                $(this).parent().parent().find('.active').removeClass('active');
                $(this).addClass('active');

                var id = this.id;
                $('div.report-content').hide();
                $('div#' + id + '-content').show();

                $('div#report-nav li').removeClass('active');
                $(this).parent().addClass('active');
            });

            // Goto first tab
            $('div#report-nav li a').first().click();

            // value of extern id, can only be set by quuery string
            // We define property 'reports' if it does not exist, but not 'extid'
            // So we have to access it as an array member (or ?? operator, but does it exist?)
            var extid = webgen.lastFilter.reports['extid'];

            // filter by event
            $('#content .filter select#events').change(function () {
                var event = $(this).val();
                var assoc = $('#content .filter select#assocs').val();
                var name = $('#content .filter input#names').val().toLowerCase();
                var startno = $('#content .filter input#startno').val();

                filterEntries(event, assoc, name, startno, extid);
            });

            // Filter by association
            $('#content .filter select#assocs').change(function () {
                var assoc = $(this).val();
                var event = $('#content .filter select#events').val();
                var name = $('#content .filter input#names').val().toLowerCase();
                var startno = $('#content .filter input#startno').val();

                filterEntries(event, assoc, name, startno, extid);
            });

            var keyDownNameTimerReports = 0;
            $('#content .filter input#names').keydown(function () {
                clearTimeout(keyDownNameTimerReports);
                keyDownNameTimerReports = setTimeout(function () {
                    var event = $('#content .filter select#events').val();
                    var assoc = $('#content .filter select#assocs').val();
                    var name = $('#content .filter input#names').val().toLowerCase();
                    var startno = $('#content .filter input#startno').val();

                    filterEntries(event, assoc, name, startno, extid);

                }, 250);
            });

            $('#content .filter input#startno').keydown(function () {
                clearTimeout(keyDownNameTimerReports);
                keyDownNameTimerReports = setTimeout(function () {
                    var event = $('#content .filter select#events').val();
                    var assoc = $('#content .filter select#assocs').val();
                    var name = $('#content .filter input#names').val().toLowerCase();
                    var startno = $('#content .filter input#startno').val();

                    filterEntries(event, assoc, name, startno, extid);

                }, 250);
            });

            // When collapsed content is shown, load the content from server
            $('#content table.report tr[data-href]').on('show.bs.collapse', function () {
                var $this = $(this);

                // For some strange reasons this function is also triggered 
                // when the next level (individual matches) are opened
                // So check if this is already open and if yes, do nothing here
                if ($this.hasClass('collapse show'))
                    return;

                // Mainly debugging: The event to be displayed.
                // All other matches are filtered out
                var cp = $this.prev().find('> td.event').text();

                // Lazy load content
                var url = (isEttu ? folderPath(filePath) + fileBasename($this.attr('data-href')) : $this.attr('data-href'));
                $.get(url, function (data) {
                    // Set content and init I18N
                    $this.find('> td').html(data).localize();

                    // When flags are to be shown replace all src attributes with the data-href- one
                    if (config.flagtype > 0) {
                        $this.find('> td table tbody tr.match span.flag img').each(function () {
                            var href = config.flagtype == 1 ? $(this).data('webgen-nation') : $(this).data('webgen-region');
                            if (href !== undefined) {
                                $(this).attr('src', 'flags/' + href + '.png');
                                $(this).attr('alt', href);
                            }
                        });
                    }

                    // If we have to filter for an event (and why shouldn't we)
                    if (cp !== '') {
                        $this.find('> td table tbody').filter(function () {
                            var text = $(this).find('tr.header th .event').text();
                            return text !== cp;
                        }).hide();
                    }

                    // Add handler for detailed results
                    $this.find('> td table.matches tr.match').not('[data-bs-toggle=collapse]').click(function (event) {
                        event.preventDefault();

                        displayGames(this);
                    });

                    $this.find('> td table.matches tr.individual').click(function (event) {
                        event.preventDefault();

                        displayGames(this);
                    });

                });
            });

            // Sort tables
            $('#content table.report').each(function () {
                var table = $(this);

                table.find('> thead th').each(function () {
                    var th = $(this),
                            thIndex = th.index(),
                            sortFunction = (th.hasClass('plnr') ? sortNumeric : sortAlpha);

                    th.click(function (event) {
                        event.preventDefault();

                        var inverse = $(this).is('.sort.up');

                        table.find('tr.report td').filter(function () {

                            return $(this).index() === thIndex;

                        }).sortElements(function (a, b) {

                            if ($(a).first('div') !== undefined) {
                                a = $(a).first('div');
                                b = $(b).first('div');
                            }

                            return inverse ? -sortFunction(a, b) : +sortFunction(a, b);

                        }, function () {

                            // parentNode is the element we want to move
                            return this.parentNode.parentNode;

                        });

                        table.find('> thead th').removeClass('sort up down');
                        th.addClass('sort').addClass(inverse ? 'down' : 'up');

                        var page = table.parent().find('div#pagination li.active a').text();
                        gotoPage(parseInt(page));
                    });
                });
            });

            gotoPage(1);

            initCollapsible($(this));

            if (webgen.lastNav === 'reports' && webgen.lastFilter.reports !== undefined) {
                $('#content div.filter select#events').val(webgen.lastFilter.reports.event);
                $('#content div.filter select#assocs').val(webgen.lastFilter.reports.assoc);
                $('#content div.filter input#names').val(webgen.lastFilter.reports.name);
                $('#content dif.filter input#startno').val(webgen.lastFilter.reports.startno);

                filterEntries(
                        webgen.lastFilter.reports.event,
                        webgen.lastFilter.reports.assoc,
                        webgen.lastFilter.reports.name,
                        webgen.lastFilter.reports.startno,
                        webgen.lastFilter.reports.extid
                        );
            }

            $('button.refresh').html(refreshSpan);
        });
    };

    if (webgen.lastNav === 'reports' && webgen.lastContent !== undefined)
        reports.load(webgen.lastContent);
};


this.liveticker = function () {
    var href = $('#' + 'liveticker').attr('data-href');

    $('#sidebar').html('');

    if ($('.row-offcanvas').hasClass('active'))
        $('[data-bs-toggle=offcanvas]').click();

    var venues = config.liveticker.venues;

    if (typeof venues === 'undefined')
        venues = ['update'];

    // Clear table for first use
    $('div#ticker-content table').remove('tbody');

    for (var i = 0; i < venues.length; i++) {
        var href = (isEttu ? folderPath(filePath) + 'js/' + venues[i] : 'js/' + venues[i]);

        ticker.update(href);
    }

};


this.news = function () {
    function gotoPage(page) {
        var maxItems = 10;

        if ($('div#news-content ul#news li').length <= maxItems) {
            $('div#news-content #pagination').html('');
            return;
        }

        $('div#news-content ul#news li').hide();
        $('div#news-content ul#news li').slice((page - 1) * maxItems, page * maxItems).show();

        var numPages = Math.floor(($('div#news-content ul#news li').length + maxItems - 1) / maxItems);
        var pagination = '<ul class="pagination">';
        if (page == 1)
            pagination += '<li class="disabled"><a class="page-first">&laquo;</a></li>';
        else
            pagination += '<li><a class="page-first">&laquo;</a></li>';

        var startPage = Math.max(page - 4, 1);
        var endPage = Math.min(page + 4, numPages);
        if (startPage > page - 4)
            endPage = Math.min(startPage + 8, numPages);
        if (endPage < page + 4)
            startPage = Math.max(endPage - 8, 1);

        var xxsStartPage = Math.max(page - 2, 1);
        var xxsEndPage = Math.min(page + 2, numPages);
        if (xxsStartPage > page - 2)
            xxsEndPage = Math.min(xxsStartPage + 4, numPages);
        if (xxsEndPage < page + 2)
            xxsStartPage = Math.max(xxsEndPage - 4, 1);

        for (var p = startPage; p <= endPage; p++) {
            if (p == page)
                pagination += '<li class="active"><a class="page-nr">' + p + '</a></li>';
            else if (p < xxsStartPage || p > xxsEndPage)
                pagination += '<li class="hidden-xxs"><a class="page-nr">' + p + '</a></li>';
            else
                pagination += '<li><a class="page-nr">' + p + '</a></li>';
        }
        if (page == numPages)
            pagination += '<li class="disabled"><a class="page-last">&raquo;</a></li>';
        else
            pagination += '<li><a class="page-last">&raquo;</a></li>';

        $('div#news-content #pagination').html(pagination);

        $('div#news-content #pagination a').click(function (event) {
            event.preventDefault();

            if ($(this).hasClass('page-first'))
                gotoPage(1);
            else if ($(this).hasClass('page-last'))
                gotoPage(numPages);
            else
                gotoPage(parseInt($(this).html()));
        });
    }

    var href = $('#' + 'news').attr('data-href');

    if (!href)
        return;

    if (isEttu)
        href = folderPath(filePath) + href;

    $('#sidebar').html('');

    if ($('.row-offcanvas').hasClass('active'))
        $('[data-bs-toggle=offcanvas]').click();

    $.ajax({
        type: 'GET',
        url: href,
        dataType: 'xml',
        success: function (xml) {
            var html =
                    '<div id="pagination"></div>' +
                    '<ul id="news" class="news list-group">';

            $(xml).find('item').each(function () {
                var $this = $(this);
                var link = $this.find('link').text();
                var title = $this.find('title').text();
                var pubDate = $this.find('pubDate').text();
                // var author = $this.find('[nodeName="dc:creator"]').text();
                var author = $this.find('dc\\:creator, creator').text();
                var description = $this.find('description').text();
                var content = undefined;

                if ($this.find('content\\:encoded, encoded') !== undefined) {
                    content = $this.find('content\\:encoded, encoded').text();
                }

                if (content === undefined || content === null || content.length == 0)
                    content = description;

                // var content = $this.find('[nodeName="content:encoded"]').html();

                if (new Date(pubDate).getYear() < new Date().getYear())
                    return;

                html +=
                        '<li class="newsitem list-group-item">' +
                        '<article>' +
                        '<h4><a href="' + link + '" target="_blank">' + title + '</a></h4>' +
                        '<h6><small><span class="pubdate">' + new Date(pubDate).toLocaleString() + '</span><span class="author">' + author + '</span></small></h6>' +
                        '<div class="description"><p>' + description + '</p></div>' +
                        '<div class="content">' + content + '</div>' +
                        '</article>' +
                        '</li>';
            });

            html += '</ul></div>';

            $('div#news-content').html(html);

            $('div#news-content li.newsitem article > div').click(function (event) {
                event.preventDefault();

                parent = $(this).parent();
                $(parent).find('> div').toggle();
            });

            gotoPage(1);
        }
    });
};



