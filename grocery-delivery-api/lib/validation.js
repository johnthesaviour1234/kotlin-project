import Joi from 'joi'

// Login request validation schema
const loginSchema = Joi.object({
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address',
    'any.required': 'Email is required'
  }),
  password: Joi.string().min(6).required().messages({
    'string.min': 'Password must be at least 6 characters long',
    'any.required': 'Password is required'
  })
})

// Registration request validation schema
const registerSchema = Joi.object({
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address',
    'any.required': 'Email is required'
  }),
  password: Joi.string().min(8).pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/).required().messages({
    'string.min': 'Password must be at least 8 characters long',
    'string.pattern.base': 'Password must contain at least one uppercase letter, one lowercase letter, and one number',
    'any.required': 'Password is required'
  }),
  full_name: Joi.string().min(2).max(50).required().messages({
    'string.min': 'Full name must be at least 2 characters long',
    'string.max': 'Full name cannot exceed 50 characters',
    'any.required': 'Full name is required'
  }),
  phone: Joi.string().pattern(/^\+?[\d\s-()]+$/).min(10).max(20).optional().messages({
    'string.pattern.base': 'Please provide a valid phone number',
    'string.min': 'Phone number must be at least 10 characters long',
    'string.max': 'Phone number cannot exceed 20 characters'
  }),
  user_type: Joi.string().valid('customer', 'admin', 'delivery_driver').default('customer').messages({
    'any.only': 'User type must be one of: customer, admin, delivery_driver'
  })
})

// Email verification schema
const verifyEmailSchema = Joi.object({
  token: Joi.string().required().messages({
    'any.required': 'Verification token is required'
  }),
  email: Joi.string().email().required().messages({
    'string.email': 'Please provide a valid email address',
    'any.required': 'Email is required'
  })
})

// Refresh token schema
const refreshTokenSchema = Joi.object({
  refresh_token: Joi.string().required().messages({
    'any.required': 'Refresh token is required'
  })
})

// Generic validation function
export const validate = (schema, data) => {
  const { error, value } = schema.validate(data, { 
    abortEarly: false,
    allowUnknown: false,
    stripUnknown: true
  })
  
  if (error) {
    return {
      isValid: false,
      errors: error.details.map(detail => ({
        field: detail.path.join('.'),
        message: detail.message
      }))
    }
  }
  
  return {
    isValid: true,
    data: value
  }
}

// Specific validation functions
export const validateLoginRequest = (data) => validate(loginSchema, data)
export const validateRegisterRequest = (data) => validate(registerSchema, data)
export const validateVerifyEmailRequest = (data) => validate(verifyEmailSchema, data)
export const validateRefreshTokenRequest = (data) => validate(refreshTokenSchema, data)

// Sanitize user data for responses (remove sensitive fields)
export const sanitizeUser = (user) => {
  if (!user) return null
  
  const { password, ...sanitizedUser } = user
  return sanitizedUser
}

// Helper function to format error responses
export const formatErrorResponse = (message, errors = null) => {
  const response = {
    success: false,
    error: message,
    timestamp: new Date().toISOString()
  }
  
  if (errors) {
    response.validation_errors = errors
  }
  
  return response
}

// Helper function to format success responses
export const formatSuccessResponse = (data, message = null) => {
  const response = {
    success: true,
    data,
    timestamp: new Date().toISOString()
  }
  
  if (message) {
    response.message = message
  }
  
  return response
}