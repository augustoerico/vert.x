/*
 * Copyright (c) 2011-2013 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *      The Eclipse Public License is available at
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 *      The Apache License v2.0 is available at
 *      http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.http.impl;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.vertx.core.http.Http2Settings.DEFAULT_ENABLE_PUSH;
import static io.vertx.core.http.Http2Settings.DEFAULT_HEADER_TABLE_SIZE;
import static io.vertx.core.http.Http2Settings.DEFAULT_INITIAL_WINDOW_SIZE;
import static io.vertx.core.http.Http2Settings.DEFAULT_MAX_CONCURRENT_STREAMS;
import static io.vertx.core.http.Http2Settings.DEFAULT_MAX_FRAME_SIZE;
import static io.vertx.core.http.Http2Settings.DEFAULT_MAX_HEADER_LIST_SIZE;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class VertxHttp2ConnectionHandlerBuilder<C extends Http2ConnectionBase> extends AbstractHttp2ConnectionHandlerBuilder<VertxHttp2ConnectionHandler<C>, VertxHttp2ConnectionHandlerBuilder<C>> {

  private Map<Channel, C> connectionMap;
  private boolean useCompression;
  private io.vertx.core.http.Http2Settings initialSettings;
  private Function<VertxHttp2ConnectionHandler<C>, C> connectionFactory;

  VertxHttp2ConnectionHandlerBuilder() {
  }

  protected VertxHttp2ConnectionHandlerBuilder<C> server(boolean isServer) {
    return super.server(isServer);
  }

  VertxHttp2ConnectionHandlerBuilder<C> connectionMap(Map<Channel, C> connectionMap) {
    this.connectionMap = connectionMap;
    return this;
  }

  VertxHttp2ConnectionHandlerBuilder<C> initialSettings(io.vertx.core.http.Http2Settings settings) {
    this.initialSettings = settings;
    return this;
  }

  VertxHttp2ConnectionHandlerBuilder<C> useCompression(boolean useCompression) {
    this.useCompression = useCompression;
    return this;
  }

  VertxHttp2ConnectionHandlerBuilder<C> connectionFactory(Function<VertxHttp2ConnectionHandler<C>, C> connectionFactory) {
    this.connectionFactory = connectionFactory;
    return this;
  }

  @Override
  protected VertxHttp2ConnectionHandler<C> build() {

    if (initialSettings != null) {
      if (!isServer() && initialSettings.getEnablePush() != DEFAULT_ENABLE_PUSH) {
        initialSettings().pushEnabled(initialSettings.getEnablePush());
      }
      if (initialSettings.getHeaderTableSize() != DEFAULT_HEADER_TABLE_SIZE) {
        initialSettings().headerTableSize(initialSettings.getHeaderTableSize());
      }
      if (initialSettings.getInitialWindowSize() != DEFAULT_INITIAL_WINDOW_SIZE) {
        initialSettings().initialWindowSize(initialSettings.getInitialWindowSize());
      }
      if (!Objects.equals(initialSettings.getMaxConcurrentStreams(), DEFAULT_MAX_CONCURRENT_STREAMS)) {
        initialSettings().maxConcurrentStreams(initialSettings.getMaxConcurrentStreams());
      }
      if (initialSettings.getMaxFrameSize() != DEFAULT_MAX_FRAME_SIZE) {
        initialSettings().maxFrameSize(initialSettings.getMaxFrameSize());
      }
      if (!Objects.equals(initialSettings.getMaxHeaderListSize(), DEFAULT_MAX_HEADER_LIST_SIZE)) {
        initialSettings().maxHeaderListSize(initialSettings.getMaxHeaderListSize());
      }
    }

    return super.build();
  }

  @Override
  protected VertxHttp2ConnectionHandler<C> build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
    if (isServer()) {
      if (useCompression) {
        encoder = new CompressorHttp2ConnectionEncoder(encoder);
      }
      VertxHttp2ConnectionHandler<C> handler = new VertxHttp2ConnectionHandler<>(connectionMap, decoder, encoder, initialSettings, connectionFactory);
      frameListener(handler.connection);
      return handler;
    } else {
      VertxHttp2ConnectionHandler<C> handler = new VertxHttp2ConnectionHandler<>(connectionMap, decoder, encoder, initialSettings, connectionFactory);
      if (useCompression) {
        frameListener(new DelegatingDecompressorFrameListener(decoder.connection(), handler.connection));
      } else {
        frameListener(handler.connection);
      }
      return handler;
    }
  }
}
