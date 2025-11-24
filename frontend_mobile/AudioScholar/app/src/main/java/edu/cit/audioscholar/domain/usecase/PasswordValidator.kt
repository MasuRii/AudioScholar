package edu.cit.audioscholar.domain.usecase

import edu.cit.audioscholar.R
import edu.cit.audioscholar.domain.model.PasswordStrength
import edu.cit.audioscholar.util.UiText

object PasswordValidator {

    fun validatePassword(password: String): Pair<PasswordStrength, List<UiText>> {
        val errors = mutableListOf<UiText>()
        if (password.length < 8) errors.add(UiText.StringResource(R.string.settings_password_validation_length))
        if (!password.any { it.isUpperCase() }) errors.add(UiText.StringResource(R.string.settings_password_validation_uppercase))
        if (!password.any { it.isLowerCase() }) errors.add(UiText.StringResource(R.string.settings_password_validation_lowercase))
        if (!password.any { it.isDigit() }) errors.add(UiText.StringResource(R.string.settings_password_validation_number))
        if (!password.any { !it.isLetterOrDigit() }) errors.add(UiText.StringResource(R.string.settings_password_validation_special))

        val strength = when {
            errors.isEmpty() -> PasswordStrength.STRONG
            errors.size <= 1 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
        
        return strength to errors
    }
}