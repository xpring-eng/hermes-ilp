package org.interledger.spsp.server.grpc.exceptions;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

public class FeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    /*switch (response.status()) {
      case 401:

    }*/
    return null;
  }
}
