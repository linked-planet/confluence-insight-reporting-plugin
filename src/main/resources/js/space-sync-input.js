/*-
 * #%L
 * insight-reporting
 * %%
 * Copyright (C) 2018 The Plugin Authors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

function initialize(spaceKey) {
    _initForm(spaceKey);
    _initSchemaChangeListener();
    _initResetButtonClickListener(spaceKey);
    _initSaveButtonClickListener(spaceKey);
}

function _initForm(spaceKey) {
    AJS.$.ajax({
        url: _configEndpointUrl(spaceKey),
        type: 'GET',
        dataType: "json",
        success: function (config) {
            _initField('objectSchemaId', null, config.schemaId);
            _initField('confluenceViewAttributeName', null, config.confluenceViewAttributeName);
            _initField('parentPageId', null, config.parentPageId);
            _initField('templateId1', null, config.templateId1);
            _initField('templateId2', null, config.templateId2);
            _initField('templateId3', null, config.templateId3);
            _initObjectTypeFields(config.schemaId, config.objectTypeId1, config.objectTypeId2, config.objectTypeId3);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            _showErrorFlag(null, errorThrown + ': ' + JSON.parse(jqXHR.responseText).message);
        }
    })
}

function _initSchemaChangeListener() {
    var $objectSchemaId = AJS.$('#objectSchemaId');
    $objectSchemaId.change(function () {
        _initObjectTypeFields($objectSchemaId.val(), null, null, null);
    })
}

function _initResetButtonClickListener(spaceKey) {
    AJS.$('#reset-button').click(function (evt) {
        evt.preventDefault();
        _initForm(spaceKey)
    });
}

function _initSaveButtonClickListener(spaceKey) {
    AJS.$('#save-button').click(function (evt) {
        evt.preventDefault();
        AJS.$.ajax({
            url: _configEndpointUrl(spaceKey),
            type: 'PUT',
            contentType: "application/json",
            data: JSON.stringify({
                schemaId: AJS.$('#objectSchemaId').val(),
                confluenceViewAttributeName: AJS.$('#confluenceViewAttributeName').val(),
                parentPageId: AJS.$('#parentPageId').val(),
                objectTypeId1: AJS.$('#objectTypeId1').val(),
                templateId1: AJS.$('#templateId1').val(),
                objectTypeId2: AJS.$('#objectTypeId2').val(),
                templateId2: AJS.$('#templateId2').val(),
                objectTypeId3: AJS.$('#objectTypeId3').val(),
                templateId3: AJS.$('#templateId3').val()
            }),
            success: function () {
                _showSuccessFlag('Settings saved.');
            },
            error: function (jqXHR) {
                _showErrorFlag('Save failed!', jqXHR.responseText);
            }
        });
    });
}

function _initObjectTypeFields(schemaId, objectTypeId1, objectTypeId2, objectTypeId3) {
    _clearSelect('objectTypeId1');
    _clearSelect('objectTypeId2');
    _clearSelect('objectTypeId3');
    AJS.$.ajax({
        url: _insightSchemaEndpointUrl(schemaId) + "/object-types",
        type: 'GET',
        dataType: "json",
        success: function (objectTypes) {
            _initField('objectTypeId1', objectTypes, objectTypeId1);
            _initField('objectTypeId2', objectTypes, objectTypeId2);
            _initField('objectTypeId3', objectTypes, objectTypeId3);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            _showErrorFlag(null, errorThrown + ': ' + JSON.parse(jqXHR.responseText).message);
        }
    });
}

function _initField(id, options, value) {
    if (options) {
        AJS.$.each(options, function (key, value) {
            AJS.$("<option/>").val(key).text(value).attr('name', value).appendTo('#' + id);
        });
    }
    if (value) {
        AJS.$('#' + id).val(value);
    }
}


function _clearSelect(id) {
    AJS.$('#' + id).empty();
    AJS.$("<option/>").val('-1').text('- none -').attr('name', 'none').appendTo('#' + id);
    AJS.$('#' + id).val('-1');
}


function _showSuccessFlag(message) {
    require(['aui/flag'], function (flag) {
        flag({type: 'success', persistent: false, body: message, close: "auto"});
    });
}

function _showErrorFlag(title, message) {
    require(['aui/flag'], function (flag) {
        flag({type: 'error', title: title, body: message});
    });
}

function _configEndpointUrl(spaceKey) {
    return AJS.contextPath() + "/rest/insight-reporting/1.0/space-sync/config/" + spaceKey;
}

function _insightSchemaEndpointUrl(schemaId) {
    return AJS.contextPath() + "/rest/insight-reporting/1.0/insight/schema/" + schemaId;
}
