package tira.predef.props

import tira.persistance.domain.Project
import tira.persistance.domain.Task

interface WithName<A> {
    fun A.name(): String
}

val projectWithNameInst = object : WithName<Project> {
    override fun Project.name(): String = name()
}
val taskWithNameInst = object : WithName<Task> {
    override fun Task.name(): String = name()
}
