/*jslint newcap: false*/
/*global $ console Column Avatar Bucket Issue StickyEditor alert*/

/*
 * Copyright (C) 2011, 2012 Commission Junction
 *
 * This file is part of etherboard.
 *
 * etherboard is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * etherboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with etherboard; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
/*global WebSocketClient */
function Board(parent, boardId) {
    var ws,
        err = function (xhr, status, error) {
          console.log(xhr, error);
          alert("ERROR: " + status);
        };

    $.ajax('/components/Board.html', {
        success: function (html) {
            var view = {},
                createSticky;

            createSticky = function (sticky) {
                $.ajax('/board/' + boardId + '/objects', {
                    dataType: 'json',
                    data: JSON.stringify(sticky),
                    type: 'POST',
                    success: function (newSticky) {
                        var msg = {
                            type: "newSticky",
                            object: newSticky
                        };
                        ws.send(JSON.stringify(msg));
                        Issue(newSticky, parent, boardId, ws);
                    },
                    error: err});
            };

            parent.append(html);

            view.title = $("#title");
            view.body = $('#board');

            view.newStickyButton = $("#newStickyButton");
              
            view.newStickyButton.button().click(function (e) {
                var newSticky = {name: "", kind: "sticky"};
                StickyEditor(newSticky, $('body'), createSticky);
            });

            view.newImageButton = $("#newImageButton");

            view.newImageButton.button().click(function () {
                var dialog = $('#newImage').dialog({title: "New image"}).show(),
                    urlField = dialog.find("input[type=text]");

                dialog.find("form").unbind("submit").submit(function (e) {
                    var img = {
                        kind: "image",
                        name: urlField.val()
                    };

                    e.preventDefault();
                    dialog.hide();
                    dialog.dialog('close');

                    $.ajax('/board/' + boardId + '/objects', {
                        dataType: 'json',
                        data: JSON.stringify(img),
                        type: 'POST',
                        success: function (newImage) {
                            urlField.val("");
                            Avatar(newImage, view.body, boardId, ws);

                            var msg = {
                                type: "newImage",
                                object: newImage
                            };
                            ws.send(JSON.stringify(msg));
                        },
                        error: err
                    });
                });
            });

            view.newBucketButton = $("#newBucketButton");
            
            view.newBucketButton.button().click(function (e) {
                var newBucket = {name: "", kind: "bucket", contents: []},
                    createBucket;

                createBucket = function (bucket) {
                    $.ajax('/board/' + boardId + '/objects', {
                        dataType: 'json',
                        data: JSON.stringify(bucket),
                        type: 'POST',
                        success: function (newBucket) {
                            Bucket(newBucket, parent, boardId, createSticky, ws);
                            var msg = {
                                type: "newBucket",
                                object: newBucket
                            };
                            ws.send(JSON.stringify(msg));
                        },
                        error: err
                    });
                };

                StickyEditor(newBucket, $('body'), createBucket);
            });

            $.ajax('/board/' + boardId + '/objects', {
                dataType: 'json',
                success: function (data) {
                    var columnCount = 1;
                    
                    var websocketUrl = data.boardUpdatesWebSocket.replace("REPLACE_ME_WITH_HOST", window.location.hostname);
                    
                    ws = WebSocketClient(websocketUrl, {
                        onMessage: function (event) {
                            var msg, newPosition, widgetId;
                            msg = JSON.parse(event.data);
                            console.log(msg.type);
                            if (msg.type === 'positionChange') {
                                console.log("processing position change msg");
                                newPosition = msg.position;
                                widgetId = msg.widgetId;
                                $("#" + widgetId).offset(newPosition);
                            } else if (msg.type === 'newSticky') {
                                console.log("processing newSticky msg");
                                Issue(msg.object, parent, boardId, ws);
                            } else if (msg.type === 'newBucket') {
                                console.log("processing newBucket msg");
                                Bucket(msg.object, parent, boardId, createSticky, ws);
                            } else if (msg.type === 'addBucketItem') {
                                widgetId = msg.widgetId;
                                console.log('processing addBucketItem msg');
                                $("#" + widgetId).trigger('addBucketItem', [msg.itemContents]);
                            } else if (msg.type === 'removeBucketItem') {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).trigger('removeBucketItem', [msg.content]);
                            } else if (msg.type === 'newImage') {
                                console.log("processing newAvatar msg");
                                Avatar(msg.object, view.body, boardId, ws);
                            } else if (msg.type === "deleteWidget") {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).remove();
                            } else if (msg.type === 'stickyContentChanged') {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).trigger("setContents", msg);
                            } else if (msg.type === 'bucketContentChanged') {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).trigger("setContents", msg);
                            } else if (msg.type === 'columnChanged') {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).trigger("setHeader", msg);
                            } else if (msg.type === 'flip') {
                                widgetId = msg.widgetId;
                                $("#" + widgetId).trigger("flip");
                            } else {
                                console.log("unknown message type");
                            }
                        }
                    });

                    view.title.append(decodeURIComponent(data.name));

                    $(data.objects).each(function (n, item) {
                        if (item.kind === "column") {
                            Column(item, view.body, boardId, ws);
                            columnCount++;
                        } else if (item.kind === "sticky") {
                            Issue(item, view.body, boardId, ws);
                        } else if (item.kind === "image") {
                            Avatar(item, view.body, boardId, ws);
                        }  else if (item.kind === "bucket") {
                            Bucket(item, view.body, boardId, createSticky, ws);
                        }
                    });

                    $("body").width(columnCount * 200);
                },
                error: err
            });
        },
        error: err
    });
}
