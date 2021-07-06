/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.sonarsource.sonarlint.core.serverapi.HttpClient;

public class SonarLintHttpClientOkHttpImpl implements HttpClient {
  private final OkHttpClient okClient;

  public SonarLintHttpClientOkHttpImpl(OkHttpClient okClient) {
    this.okClient = okClient;
  }

  @Override
  public Response post(String url, String contentType, String bodyContent) {
    RequestBody body = RequestBody.create(MediaType.get(contentType), bodyContent);
    Request request = new Request.Builder()
      .url(url)
      .post(body)
      .build();
    return executeRequest(request);
  }

  @Override
  public Response get(String url) {
    Request request = new Request.Builder()
      .url(url)
      .build();
    return executeRequest(request);
  }

  @Override
  public CompletableFuture<Response> getAsync(String url) {
    Request request = new Request.Builder()
      .url(url)
      .build();
    return executeRequestAsync(request);
  }

  @Override
  public Response delete(String url, String contentType, String bodyContent) {
    RequestBody body = RequestBody.create(MediaType.get(contentType), bodyContent);
    Request request = new Request.Builder()
      .url(url)
      .delete(body)
      .build();
    return executeRequest(request);
  }

  private Response executeRequest(Request request) {
    try {
      return wrap(okClient.newCall(request).execute());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to execute request: " + e.getMessage(), e);
    }
  }

  private CompletableFuture<Response> executeRequestAsync(Request request) {
    Call call = okClient.newCall(request);
    CompletableFuture<Response> futureResponse = new CompletableFuture<Response>()
      .whenComplete((response, error) -> {
        if (error instanceof CancellationException) {
          call.cancel();
        }
      });
    call.enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        futureResponse.completeExceptionally(e);
      }

      @Override
      public void onResponse(Call call, okhttp3.Response response) {
        futureResponse.complete(wrap(response));
      }
    });
    return futureResponse;
  }

  private static Response wrap(okhttp3.Response wrapped) {
    return new Response() {

      @Override
      public String url() {
        return wrapped.request().url().toString();
      }

      @Override
      public int code() {
        return wrapped.code();
      }

      @Override
      public void close() {
        wrapped.close();
      }

      @Override
      public String bodyAsString() {
        try (ResponseBody body = wrapped.body()) {
          return body.string();
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read response body: " + e.getMessage(), e);
        }
      }

      @Override
      public InputStream bodyAsStream() {
        return wrapped.body().byteStream();
      }

      @Override
      public String toString() {
        return wrapped.toString();
      }
    };
  }
}
