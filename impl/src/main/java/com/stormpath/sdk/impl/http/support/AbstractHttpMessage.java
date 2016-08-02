/*
 * Copyright 2013 Stormpath, Inc.
 *
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
 */
package com.stormpath.sdk.impl.http.support;

import com.stormpath.sdk.http.HttpMessage;

import java.io.InputStream;

/**
 * @since 0.1
 */
public abstract class AbstractHttpMessage implements HttpMessage {

    @Override
    public boolean hasBody() {
        InputStream is = getBody();
        return is != null && getHeaders().getContentLength() != 0;
    }
}
