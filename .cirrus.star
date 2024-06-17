load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs", "yaml")


def main(ctx):
  if env.get("CIRRUS_BRANCH") != None:
    if env.get("CIRRUS_BRANCH").startswith("ibuilds-"):
      return yaml.dumps(load_features(ctx)) + fs.read(".cirrus.ibuilds.yml")
  return yaml.dumps(load_features(ctx, aws=dict(env_type="dev"))) + fs.read(".cirrus.default.yml")
