package com.alertyai.app.ui.chat;

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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<TaskRepository> taskRepositoryProvider;

  public ChatViewModel_Factory(Provider<TaskRepository> taskRepositoryProvider) {
    this.taskRepositoryProvider = taskRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(taskRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<TaskRepository> taskRepositoryProvider) {
    return new ChatViewModel_Factory(taskRepositoryProvider);
  }

  public static ChatViewModel newInstance(TaskRepository taskRepository) {
    return new ChatViewModel(taskRepository);
  }
}
