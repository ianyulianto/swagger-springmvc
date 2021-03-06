package com.mangofactory.swagger.readers.operation;

import com.fasterxml.classmate.ResolvedType;
import com.mangofactory.swagger.configuration.SwaggerGlobalSettings;
import com.mangofactory.swagger.models.ResolvedTypes;
import com.mangofactory.swagger.readers.Command;
import com.mangofactory.swagger.scanners.RequestMappingContext;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.model.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.*;
import static com.mangofactory.swagger.ScalaUtils.*;
import static com.mangofactory.swagger.core.ModelUtils.*;

public class OperationResponseMessageReader implements Command<RequestMappingContext> {
   @Override
   public void execute(RequestMappingContext context) {
      SwaggerGlobalSettings swaggerGlobalSettings = (SwaggerGlobalSettings) context.get("swaggerGlobalSettings");
      RequestMethod currentHttpMethod = (RequestMethod) context.get("currentHttpMethod");
      HandlerMethod handlerMethod = context.getHandlerMethod();
      List<ResponseMessage> responseMessages = newArrayList();
      ApiResponses apiResponses = getAnnotatedResponseMessages(handlerMethod.getMethod());

      if (null != apiResponses) {
         ApiResponse[] apiResponseAnnotations = apiResponses.value();
         for (ApiResponse apiResponse : apiResponseAnnotations) {
            responseMessages.add(new ResponseMessage(apiResponse.code(), apiResponse.message(), toOption(null)));
         }
      } else {
         Map<RequestMethod, List<ResponseMessage>> globalResponseMessages = swaggerGlobalSettings.getGlobalResponseMessages();
         if (null != globalResponseMessages) {
            responseMessages = globalResponseMessages.get(currentHttpMethod);
         }
         ResolvedType returnType = handlerReturnType(swaggerGlobalSettings.getTypeResolver(), handlerMethod);
         returnType = swaggerGlobalSettings.getAlternateTypeProvider().alternateFor(returnType);
         if (Void.class != returnType.getErasedType()) {
            String simpleName = ResolvedTypes.typeName(returnType);
            ResponseMessage responseWithModel = new ResponseMessage(200, HttpStatus.OK.getReasonPhrase(), toOption(simpleName));
            safelyRemoveHttpOkResponse(responseMessages);
            responseMessages.add(responseWithModel);
         }
      }
      context.put("responseMessages", responseMessages);
   }

   private ApiResponses getAnnotatedResponseMessages(Method method) {
      Annotation[] methodAnnotations = method.getAnnotations();
      if (null != methodAnnotations) {
         for (Annotation annotation : methodAnnotations) {
            if (annotation instanceof ApiResponses) {
               return (ApiResponses) annotation;
            }
         }
      }
      return null;
   }

   private void safelyRemoveHttpOkResponse(List<ResponseMessage> responseMessages) {
      //Safely remove using iterator
      if (null != responseMessages && responseMessages.size() > 0) {
         Iterator<ResponseMessage> i = responseMessages.iterator();
         while (i.hasNext()) {
            ResponseMessage msg = i.next();
            if (msg.code() == 200) {
               i.remove();
               break;
            }
         }
      }
   }
}
