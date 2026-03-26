package com.xindai.xindai.common.exception;

import com.xindai.xindai.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Nested
    @DisplayName("业务异常处理测试")
    class BusinessExceptionTests {

        @Test
        @DisplayName("处理BusinessException - 使用ErrorCode")
        void handleBusinessException_WithErrorCode() {
            BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

            ResponseEntity<Result<Void>> response = exceptionHandler.handleBusinessException(exception);

            assertNotNull(response);
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), response.getBody().getCode());
            assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), response.getBody().getMessage());
        }

        @Test
        @DisplayName("处理BusinessException - 使用消息")
        void handleBusinessException_WithMessage() {
            BusinessException exception = new BusinessException("自定义错误消息");

            ResponseEntity<Result<Void>> response = exceptionHandler.handleBusinessException(exception);

            assertNotNull(response);
            assertEquals(500, response.getBody().getCode());
            assertEquals("自定义错误消息", response.getBody().getMessage());
        }

        @Test
        @DisplayName("处理BusinessException - 使用code和message")
        void handleBusinessException_WithCodeAndMessage() {
            BusinessException exception = new BusinessException(400, "请求参数错误");

            ResponseEntity<Result<Void>> response = exceptionHandler.handleBusinessException(exception);

            assertNotNull(response);
            assertEquals(400, response.getBody().getCode());
            assertEquals("请求参数错误", response.getBody().getMessage());
        }

        @Test
        @DisplayName("处理BusinessException - 验证HTTP状态码映射")
        void handleBusinessException_HttpStatusMapping() {
            // 400级别错误码
            BusinessException badRequest = new BusinessException(400, "Bad Request");
            ResponseEntity<Result<Void>> response400 = exceptionHandler.handleBusinessException(badRequest);
            assertEquals(HttpStatus.BAD_REQUEST, response400.getStatusCode());

            // 401错误码
            BusinessException unauthorized = new BusinessException(401, "Unauthorized");
            ResponseEntity<Result<Void>> response401 = exceptionHandler.handleBusinessException(unauthorized);
            assertEquals(HttpStatus.UNAUTHORIZED, response401.getStatusCode());

            // 404错误码
            BusinessException notFound = new BusinessException(404, "Not Found");
            ResponseEntity<Result<Void>> response404 = exceptionHandler.handleBusinessException(notFound);
            assertEquals(HttpStatus.NOT_FOUND, response404.getStatusCode());
        }

        @Test
        @DisplayName("处理BusinessException - 业务错误码映射")
        void handleBusinessException_BusinessCodeMapping() {
            // 用户模块错误 (1xxx)
            BusinessException userError = new BusinessException(ErrorCode.USER_NOT_FOUND);
            ResponseEntity<Result<Void>> response = exceptionHandler.handleBusinessException(userError);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            // 借贷模块错误 (2xxx)
            BusinessException loanError = new BusinessException(ErrorCode.LIMIT_INSUFFICIENT);
            ResponseEntity<Result<Void>> loanResponse = exceptionHandler.handleBusinessException(loanError);
            assertEquals(HttpStatus.BAD_REQUEST, loanResponse.getStatusCode());

            // 风控模块错误 (3xxx)
            BusinessException riskError = new BusinessException(ErrorCode.RISK_ASSESSMENT_FAILED);
            ResponseEntity<Result<Void>> riskResponse = exceptionHandler.handleBusinessException(riskError);
            assertEquals(HttpStatus.BAD_REQUEST, riskResponse.getStatusCode());
        }
    }

    @Nested
    @DisplayName("参数校验异常处理测试")
    class ValidationExceptionTests {

        @Test
        @DisplayName("处理MethodArgumentNotValidException")
        void handleValidationException() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
            FieldError fieldError = new FieldError("object", "phone", "手机号不能为空");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            Result<Void> result = exceptionHandler.handleValidationException(exception);

            assertNotNull(result);
            assertEquals(400, result.getCode());
            assertTrue(result.getMessage().contains("手机号不能为空"));
        }

        @Test
        @DisplayName("处理多个字段校验错误")
        void handleValidationException_MultipleErrors() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
            FieldError error1 = new FieldError("object", "phone", "手机号不能为空");
            FieldError error2 = new FieldError("object", "password", "密码不能为空");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

            Result<Void> result = exceptionHandler.handleValidationException(exception);

            assertNotNull(result);
            assertEquals(400, result.getCode());
            assertTrue(result.getMessage().contains("手机号不能为空"));
            assertTrue(result.getMessage().contains("密码不能为空"));
        }

        @Test
        @DisplayName("处理BindException")
        void handleBindException() {
            BindException exception = mock(BindException.class);
            FieldError fieldError = new FieldError("object", "amount", "金额必须大于0");

            when(exception.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
            when(exception.getBindingResult().getFieldErrors()).thenReturn(List.of(fieldError));

            Result<Void> result = exceptionHandler.handleBindException(exception);

            assertNotNull(result);
            assertEquals(400, result.getCode());
            assertTrue(result.getMessage().contains("金额必须大于0"));
        }
    }

    @Nested
    @DisplayName("通用异常处理测试")
    class GeneralExceptionTests {

        @Test
        @DisplayName("处理通用Exception")
        void handleException() {
            Exception exception = new RuntimeException("系统内部错误");

            Result<Void> result = exceptionHandler.handleException(exception);

            assertNotNull(result);
            assertEquals(500, result.getCode());
            assertEquals("系统繁忙，请稍后重试", result.getMessage());
        }

        @Test
        @DisplayName("处理NullPointerException")
        void handleException_NullPointer() {
            Exception exception = new NullPointerException("Null value");

            Result<Void> result = exceptionHandler.handleException(exception);

            assertNotNull(result);
            assertEquals(500, result.getCode());
            // 不应该暴露内部错误细节
            assertEquals("系统繁忙，请稍后重试", result.getMessage());
        }

        @Test
        @DisplayName("处理IllegalArgumentException")
        void handleException_IllegalArgument() {
            Exception exception = new IllegalArgumentException("Invalid argument");

            Result<Void> result = exceptionHandler.handleException(exception);

            assertNotNull(result);
            assertEquals(500, result.getCode());
            assertEquals("系统繁忙，请稍后重试", result.getMessage());
        }
    }

    @Nested
    @DisplayName("BusinessException构造器测试")
    class BusinessExceptionConstructorTests {

        @Test
        @DisplayName("ErrorCode构造器")
        void constructor_ErrorCode() {
            BusinessException exception = new BusinessException(ErrorCode.PHONE_EXISTS);

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());
            assertEquals(ErrorCode.PHONE_EXISTS.getMessage(), exception.getMessage());
            assertNotNull(exception.getHttpStatus());
        }

        @Test
        @DisplayName("ErrorCode和detail构造器")
        void constructor_ErrorCodeAndDetail() {
            BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND, "id=123");

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("id=123"));
        }

        @Test
        @DisplayName("消息构造器")
        void constructor_Message() {
            BusinessException exception = new BusinessException("自定义错误");

            assertEquals(500, exception.getCode());
            assertEquals("自定义错误", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }

        @Test
        @DisplayName("code和message构造器")
        void constructor_CodeAndMessage() {
            BusinessException exception = new BusinessException(1001, "用户不存在");

            assertEquals(1001, exception.getCode());
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("message和HttpStatus构造器")
        void constructor_MessageAndHttpStatus() {
            BusinessException exception = new BusinessException("禁止访问", HttpStatus.FORBIDDEN);

            assertEquals(403, exception.getCode());
            assertEquals("禁止访问", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }
    }
}
