package com.alertyai.app.ui.tasks;

import com.alertyai.app.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TasksViewModel_Factory implements Factory<TasksViewModel> {
  private final Provider<TaskRepository> repositoryProvider;

  public TasksViewModel_Factory(Provider<TaskRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TasksViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TasksViewModel_Factory create(Provider<TaskRepository> repositoryProvider) {
    return new TasksViewModel_Factory(repositoryProvider);
  }

  public static TasksViewModel newInstance(TaskRepository repository) {
    return new TasksViewModel(repository);
  }
}
