/* Copyright (C) 2020 Christoph Theis */

/*
 * Write here tournament specific scripts
 */

/* global i18n */

WebFontConfig = {
  google: { families: [ 'Open+Sans+Condensed:300:latin' ] }
};
(function() {
  var wf = document.createElement('script');
  wf.src = ('https:' == document.location.protocol ? 'https' : 'http') +
    '://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js';
  wf.type = 'text/javascript';
  wf.async = 'true';
  var s = document.getElementsByTagName('script')[0];
  s.parentNode.insertBefore(wf, s);
})();

$(document).ready(function() {
  i18n.setLng('en-US', { fixLng: true }, function(enUS) { $(document).i18n(); });
});
