package com.alertyai.app.ui.teams;

import com.alertyai.app.data.repository.OrgRepository;
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
public final class TeamChatViewModel_Factory implements Factory<TeamChatViewModel> {
  private final Provider<OrgRepository> repositoryProvider;

  public TeamChatViewModel_Factory(Provider<OrgRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TeamChatViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TeamChatViewModel_Factory create(Provider<OrgRepository> repositoryProvider) {
    return new TeamChatViewModel_Factory(repositoryProvider);
  }

  public static TeamChatViewModel newInstance(OrgRepository repository) {
    return new TeamChatViewModel(repository);
  }
}
