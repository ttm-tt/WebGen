$(document).ready(function() {
    let scanner = '<div><input id="scanner" type="text"></div>';
    $('div#content').after($(scanner));
    $('#scanner').focus();
    $('#scanner').on('blur', function(e) {
        e.preventDefault();
        $(this).focus();
    });
    $('#scanner').on('change', function() {
        // Reload the page with new filter
        window.location.href = window.location.href.split('?')[0] + '?nav=reports&filterStartNo=' + $('#scanner').val();
    });
    
    // Wait a bit until page contnt is loaded , too, before showing the rows
    setTimeout(function() {$('table tbody').not('.filtered').find('tr').collapse('show');}, 100);
});