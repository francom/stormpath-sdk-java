<%--
  ~ Copyright 2014 Stormpath, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="t" uri="http://stormpath.com/jsp/tags/templates" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:page>
    <jsp:attribute name="title">Account Verified</jsp:attribute>
    <jsp:attribute name="bodyCssClass">login</jsp:attribute>
    <jsp:attribute name="timedRedirectSeconds">${timedRedirectSeconds}</jsp:attribute>
    <jsp:attribute name="timedRedirectUrl">${timedRedirectUrl}</jsp:attribute>
    <jsp:body>
        <div class="container custom-container">
            <div class="va-wrapper">
                <div class="view login-view container">
                    <div class="box row">
                        <div class="email-password-area col-xs-12 large col-sm-12">
                            <div class="header">
                                <span>Account Verification Complete!</span>
                                <p>Your account has been verified, and you have been logged into
                                    your account. You will be redirected back to the site in ${timedRedirectSeconds}
                                    seconds.</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </jsp:body>
</t:page>