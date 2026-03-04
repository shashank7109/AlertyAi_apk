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
public final class TeamsViewModel_Factory implements Factory<TeamsViewModel> {
  private final Provider<OrgRepository> repositoryProvider;

  public TeamsViewModel_Factory(Provider<OrgRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TeamsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TeamsViewModel_Factory create(Provider<OrgRepository> repositoryProvider) {
    return new TeamsViewModel_Factory(repositoryProvider);
  }

  public static TeamsViewModel newInstance(OrgRepository repository) {
    return new TeamsViewModel(repository);
  }
}
