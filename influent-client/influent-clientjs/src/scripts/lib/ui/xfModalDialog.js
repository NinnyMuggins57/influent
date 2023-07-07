/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
define(['jquery'],
    function($) {
        return {
            createInstance : function(spec) {
                // Dialog will close when any button is pressed so developer doesn't have to remember to call
                // $(this).dialog("close") for each0 button they write
                var autoCloseButtons = {};
                for (var key in spec.buttons) {
                    if (spec.buttons.hasOwnProperty(key)) {
                        (function(key) {
                            if (spec.buttons.hasOwnProperty(key)) {
                                autoCloseButtons[key] = function() {
                                    spec.buttons[key]();
                                    $(this).dialog("close");
                                };
                            }
                        })(key);
                    }
                }

                var dialogContents =  '<div id="dialog-confirm">'
                    +   '<p><span class="ui-icon ui-icon-alert" style="float: left; margin: 0 7px 20px 0;"></span>' + spec.contents + '</p>'
                    + '</div>';
                var dialogDiv = $('<div></div>');

                dialogDiv.html(dialogContents);
                $('body').append(dialogDiv);
                dialogDiv.dialog({
                    zIndex:30000000,
                    minHeight:'auto',
                    resizable:false,
                    modal:true,
                    title:spec.title,
                    // These two lines prevent the "x" from appearing in the corner of dialogs.   It's annoying to handle closing the dialog
                    // this way so this is how we can prevent the user from doing this
                    closeOnEscape: false,
                    open: function(event, ui) { $(this).parent().children().children('.ui-dialog-titlebar-close').hide(); },
                    buttons: autoCloseButtons,
                    resizeStop: function(event, ui) {
                    },
                    close: function(event, ui) {
                        $(this).dialog("destroy");
                    }
                });
            }
        };
    }
);