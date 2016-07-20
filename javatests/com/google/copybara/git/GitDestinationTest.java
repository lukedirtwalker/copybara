import static com.google.common.truth.Truth.assertAbout;
import static com.google.copybara.git.GitRepository.CURRENT_PROCESS_ENVIRONMENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import com.google.common.collect.ImmutableList;
import com.google.copybara.Author;
import com.google.copybara.Destination;
import com.google.copybara.Destination.WriterResult;
import com.google.copybara.TransformResult;
import com.google.copybara.testing.DummyOrigin;
import com.google.copybara.testing.DummyReference;
import com.google.copybara.testing.LogSubjects;
import com.google.copybara.testing.TransformResults;
import com.google.copybara.util.console.testing.TestingConsole;
import com.google.copybara.util.console.testing.TestingConsole.MessageType;
import java.io.IOException;
  private TestingConsole console;
  private ImmutableList<String> excludedDestinationPaths;
    console = new TestingConsole();
    options = new OptionsBuilder().setConsole(console);
    excludedDestinationPaths = ImmutableList.of();
    return new GitRepository(repoGitDir, /*workTree=*/null, /*verbose=*/true,
        CURRENT_PROCESS_ENVIRONMENT);
    return destinationFirstCommit(/*askConfirmation*/ false);
  }

  private GitDestination destinationFirstCommit(boolean askConfirmation)
      throws ConfigValidationException {
    return yaml.withOptions(options.build(), CONFIG_NAME, askConfirmation);
    return yaml.withOptions(options.build(), CONFIG_NAME, /*askConfirmation*/ false);
        .contains("\n    " + DummyOrigin.LABEL_NAME + ": " + originRef + "\n");
  }

  private void assertCommitHasAuthor(String branch, Author author) throws RepoException {
    assertThat(git("--git-dir", repoGitDir.toString(), "log", "-n1",
        "--pretty=format:\"%an <%ae>\"", branch))
        .isEqualTo("\"" + author + "\"");
  }

  private void process(GitDestination destination, DummyReference originRef)
      throws ConfigValidationException, RepoException, IOException {
    processWithBaseline(destination, originRef, /*baseline=*/ null);
  }

  private void processWithBaseline(GitDestination destination, DummyReference originRef,
      String baseline)
      throws RepoException, ConfigValidationException, IOException {
    TransformResult result = TransformResults.of(workdir, originRef, excludedDestinationPaths);
    if (baseline != null) {
      result = result.withBaseline(baseline);
    }
    WriterResult destinationResult = destination.newWriter().write(result, console);
    assertThat(destinationResult).isEqualTo(WriterResult.OK);
    process(destinationFirstCommit(), new DummyReference("origin_ref"));
  @Test
  public void processUserAborts() throws Exception {
    console = new TestingConsole()
        .respondNo();
    yaml.setFetch("master");
    yaml.setPush("master");
    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    thrown.expect(RepoException.class);
    thrown.expectMessage("User aborted execution: did not confirm diff changes");
    process(destinationFirstCommit(/*askConfirmation*/ true), new DummyReference("origin_ref"));
  }

  @Test
  public void processUserConfirms() throws Exception {
    console = new TestingConsole()
        .respondYes();
    yaml.setFetch("master");
    yaml.setPush("master");
    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    process(destinationFirstCommit(/*askConfirmation*/ true), new DummyReference("origin_ref"));

    assertAbout(LogSubjects.console())
        .that(console)
        .matchesNext(MessageType.PROGRESS, "Git Destination: Fetching file:.*")
        .matchesNext(MessageType.PROGRESS, "Git Destination: Adding files for push")
        .equalsNext(MessageType.INFO, "\n"
            + "diff --git a/test.txt b/test.txt\n"
            + "new file mode 100644\n"
            + "index 0000000..f0eec86\n"
            + "--- /dev/null\n"
            + "+++ b/test.txt\n"
            + "@@ -0,0 +1 @@\n"
            + "+some content\n"
            + "\\ No newline at end of file\n")
        .matchesNext(MessageType.WARNING, "Proceed with push to.*[?]")
        .matchesNext(MessageType.PROGRESS, "Git Destination: Pushing to .*")
        .containsNoMoreMessages();
  }

    DummyReference ref = new DummyReference("origin_ref");
    process(destinationFirstCommit(), ref);
    process(destination(), ref);
    process(destination(), new DummyReference("origin_ref"));
    process(destinationFirstCommit(), new DummyReference("origin_ref"));
    process(destination(), new DummyReference("origin_ref"));
    assertThat(destination1.getPreviousRef(DummyOrigin.LABEL_NAME)).isNull();
    process(destination1, new DummyReference("first_commit"));
    assertThat(destination2.getPreviousRef(DummyOrigin.LABEL_NAME)).isEqualTo("first_commit");
    process(destination2, new DummyReference("second_commit"));
    assertThat(destination3.getPreviousRef(DummyOrigin.LABEL_NAME)).isEqualTo("second_commit");
    process(destination3, new DummyReference("third_commit"));
  @Test
  public void previousImportReference_nonCopybaraCommitsSinceLastMigrate() throws Exception {
    process(destinationFirstCommit(), new DummyReference("first_commit"));

    Path scratchTree = Files.createTempDirectory("GitDestinationTest-scratchTree");
    for (int i = 0; i < 20; i++) {
      Files.write(scratchTree.resolve("excluded.dat"), new byte[] {(byte) i});
      repo().withWorkTree(scratchTree)
          .simpleCommand("add", "excluded.dat");
      repo().withWorkTree(scratchTree)
          .simpleCommand("commit", "-m", "excluded #" + i);
    }

    assertThat(destination().getPreviousRef(DummyOrigin.LABEL_NAME))
        .isEqualTo("first_commit");
  public void previousImportReferenceIsBeforeACommitWithMultipleParents() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");
    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    process(destinationFirstCommit(), new DummyReference("first_commit"));
    Path scratchTree = Files.createTempDirectory("GitDestinationTest-scratchTree");
    GitRepository scratchRepo = repo().withWorkTree(scratchTree);
    scratchRepo.simpleCommand("checkout", "-b", "b1");
    Files.write(scratchTree.resolve("b1.file"), new byte[] {1});
    scratchRepo.simpleCommand("add", "b1.file");
    scratchRepo.simpleCommand("commit", "-m", "b1");
    scratchRepo.simpleCommand("checkout", "-b", "b2", "master");
    Files.write(scratchTree.resolve("b2.file"), new byte[] {2});
    scratchRepo.simpleCommand("add", "b2.file");
    scratchRepo.simpleCommand("commit", "-m", "b2");
    scratchRepo.simpleCommand("checkout", "master");
    scratchRepo.simpleCommand("merge", "b1");
    scratchRepo.simpleCommand("merge", "b2");
    thrown.expect(RepoException.class);
    thrown.expectMessage(
        "Found commit with multiple parents (merge commit) when looking for "
        + DummyOrigin.LABEL_NAME + ".");
    destination().getPreviousRef(DummyOrigin.LABEL_NAME);
    process(destinationFirstCommit(), new DummyReference("first_commit").withTimestamp(1414141414));
    process(destination(), new DummyReference("second_commit").withTimestamp(1515151515));
    process(destinationFirstCommit(), new DummyReference("first_commit").withTimestamp(1414141414));
    process(destination(), new DummyReference("second_commit").withTimestamp(1414141490));

    process(destinationFirstCommit(), new DummyReference("first_commit").withTimestamp(1414141414));
    process(destination(), new DummyReference("second_commit").withTimestamp(1414141490));
    process(destinationFirstCommit(), new DummyReference("first_commit"));
    process(destinationFirstCommit(), new DummyReference("first_commit"));
  }

  @Test
  public void authorPropagated() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");

    Files.write(workdir.resolve("test.txt"), "some content".getBytes());

    DummyReference firstCommit = new DummyReference("first_commit")
        .withAuthor(new Author("Foo Bar", "foo@bar.com"))
        .withTimestamp(1414141414);
    process(destinationFirstCommit(), firstCommit);

    assertCommitHasAuthor("master", new Author("Foo Bar", "foo@bar.com"));
  }

  @Test
  public void canExcludeDestinationPathFromWorkflow() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");

    Path scratchTree = Files.createTempDirectory("GitDestinationTest-scratchTree");
    Files.write(scratchTree.resolve("excluded.txt"), "some content".getBytes(UTF_8));
    repo().withWorkTree(scratchTree)
        .simpleCommand("add", "excluded.txt");
    repo().withWorkTree(scratchTree)
        .simpleCommand("commit", "-m", "message");

    Files.write(workdir.resolve("normal_file.txt"), "some more content".getBytes(UTF_8));
    excludedDestinationPaths = ImmutableList.of("excluded.txt");
    process(destination(), new DummyReference("ref"));
    GitTesting.assertThatCheckout(repo(), "master")
        .containsFile("excluded.txt", "some content")
        .containsFile("normal_file.txt", "some more content")
        .containsNoMoreFiles();
  }

  @Test
  public void excludedDestinationPathsIgnoreGitTreeFiles() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");

    Path scratchTree = Files.createTempDirectory("GitDestinationTest-scratchTree");
    Files.createDirectories(scratchTree.resolve("notgit"));
    Files.write(scratchTree.resolve("notgit/HEAD"), "some content".getBytes(UTF_8));
    repo().withWorkTree(scratchTree)
        .simpleCommand("add", "notgit/HEAD");
    repo().withWorkTree(scratchTree)
        .simpleCommand("commit", "-m", "message");

    Files.write(workdir.resolve("normal_file.txt"), "some more content".getBytes(UTF_8));

    // Make sure this glob does not cause .git/HEAD to be added.
    excludedDestinationPaths = ImmutableList.of("**/HEAD");

    process(destination(), new DummyReference("ref"));
    GitTesting.assertThatCheckout(repo(), "master")
        .containsFile("notgit/HEAD", "some content")
        .containsFile("normal_file.txt", "some more content")
        .containsNoMoreFiles();
  }

  @Test
  public void processWithBaseline() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");
    DummyReference ref = new DummyReference("origin_ref");

    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    process(destinationFirstCommit(), ref);
    String firstCommit = repo().revParse("HEAD");
    Files.write(workdir.resolve("test.txt"), "new content".getBytes());
    process(destination(), ref);

    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    Files.write(workdir.resolve("other.txt"), "other file".getBytes());
    processWithBaseline(destination(), ref, firstCommit);

    GitTesting.assertThatCheckout(repo(), "master")
        .containsFile("test.txt", "new content")
        .containsFile("other.txt", "other file")
        .containsNoMoreFiles();
  }

  @Test
  public void processWithBaselineSameFileConflict() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");
    DummyReference ref = new DummyReference("origin_ref");

    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    process(destinationFirstCommit(), ref);
    String firstCommit = repo().revParse("HEAD");
    Files.write(workdir.resolve("test.txt"), "new content".getBytes());
    process(destination(), ref);

    Files.write(workdir.resolve("test.txt"), "conflict content".getBytes());
    thrown.expect(RebaseConflictException.class);
    thrown.expectMessage("conflict in test.txt");
    processWithBaseline(destination(), ref, firstCommit);
  }

  @Test
  public void processWithBaselineSameFileNoConflict() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");
    String text = "";
    for (int i = 0; i < 1000; i++) {
      text += "Line " + i + "\n";
    }
    DummyReference ref = new DummyReference("origin_ref");

    Files.write(workdir.resolve("test.txt"), text.getBytes());
    process(destinationFirstCommit(), ref);
    String firstCommit = repo().revParse("HEAD");
    Files.write(workdir.resolve("test.txt"),
        text.replace("Line 200", "Line 200 Modified").getBytes());
    process(destination(), ref);

    Files.write(workdir.resolve("test.txt"),
        text.replace("Line 500", "Line 500 Modified").getBytes());

    processWithBaseline(destination(), ref, firstCommit);

    GitTesting.assertThatCheckout(repo(), "master").containsFile("test.txt",
        text.replace("Line 200", "Line 200 Modified")
            .replace("Line 500", "Line 500 Modified")).containsNoMoreFiles();
  }

  @Test
  public void pushSequenceOfChangesToReviewBranch() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("refs_for_master");

    Destination.Writer writer = destinationFirstCommit().newWriter();

    Files.write(workdir.resolve("test42"), "42".getBytes(UTF_8));
    WriterResult result = writer.write(TransformResults.of(workdir, new DummyReference("ref1")), console);
    assertThat(result).isEqualTo(WriterResult.OK);
    String firstCommitHash = repo().simpleCommand("rev-parse", "refs_for_master").getStdout();

    Files.write(workdir.resolve("test99"), "99".getBytes(UTF_8));
    result = writer.write(TransformResults.of(workdir, new DummyReference("ref2")), console);
    assertThat(result).isEqualTo(WriterResult.OK);

    // Make sure parent of second commit is the first commit.
    assertThat(repo().simpleCommand("rev-parse", "refs_for_master~1").getStdout())
        .isEqualTo(firstCommitHash);

    // Make sure commits have correct file content.
    GitTesting.assertThatCheckout(repo(), "refs_for_master~1")
        .containsFile("test42", "42")
        .containsNoMoreFiles();
    GitTesting.assertThatCheckout(repo(), "refs_for_master")
        .containsFile("test42", "42")
        .containsFile("test99", "99")
        .containsNoMoreFiles();