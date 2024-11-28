package tira.predef.props

import tira.persistance.domain.Property

interface WithProperties {
    fun props(): List<Property>
}
