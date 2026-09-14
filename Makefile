.PHONY: install build test lint format-check mapper-check schema-check seed-demo seed-random-posts agent-install agent-run post-review-install post-review-run recommend-install recommend-run governance-install governance-run agents-install e2e verify ci

JAVA_DIR := backend
FRONT_DIR := frontend

install:
	npm --prefix $(FRONT_DIR) run install:all

build:
	cd $(JAVA_DIR) && ./mvnw -DskipTests package
	npm --prefix $(FRONT_DIR) run build

test:
	cd $(JAVA_DIR) && ./mvnw test
	npm --prefix $(FRONT_DIR) run test

lint:
	npm --prefix $(FRONT_DIR) run lint

format-check:
	npm --prefix $(FRONT_DIR) run format:check

mapper-check:
	@if rg -n '@(Select|Insert|Update|Delete|Options)' $(JAVA_DIR)/zentide-common/src/main/java/com/zentide/mapper --glob '*.java'; then \
		echo "Domain Mapper SQL must live in XML files." >&2; \
		exit 1; \
	fi

schema-check:
	./scripts/verify-schema.sh

seed-demo:
	./scripts/seed-community-demo.sh

seed-random-posts:
	./scripts/seed-random-posts.sh $(ARGS)

agent-install:
	$(if $(ZENTIDE_AGENT_PYTHON),$(ZENTIDE_AGENT_PYTHON),python3) -m pip install -r $(JAVA_DIR)/agent/src/main/python/requirements-agents.txt

agent-run:
	./scripts/run-agents.sh

post-review-install:
	$(if $(ZENTIDE_AGENT_PYTHON),$(ZENTIDE_AGENT_PYTHON),python3) -m pip install -r $(JAVA_DIR)/agent/src/main/python/SmartPosting/requirements.txt

post-review-run:
	./scripts/run-post-review.sh

recommend-install:
	$(if $(ZENTIDE_AGENT_PYTHON),$(ZENTIDE_AGENT_PYTHON),python3) -m pip install -r $(JAVA_DIR)/agent/src/main/python/Recommend/requirements.txt

recommend-run:
	./scripts/run-recommend.sh

governance-install:
	$(if $(ZENTIDE_AGENT_PYTHON),$(ZENTIDE_AGENT_PYTHON),python3) -m pip install -r $(JAVA_DIR)/agent/src/main/python/Governance/requirements.txt

governance-run:
	./scripts/run-governance.sh

agents-install:
	# --clear repairs/recreates stale virtualenvs created by another Python distribution.
	# This is intentional: the unified Agent environment is disposable and rebuilt here.
	python3 -m venv --clear .venv-agents
	.venv-agents/bin/python -m pip install --upgrade pip
	.venv-agents/bin/python -m pip install -r $(JAVA_DIR)/agent/src/main/python/requirements-agents.txt

e2e:
	npm --prefix $(FRONT_DIR) run test:e2e

verify: lint format-check mapper-check test build

ci: schema-check verify e2e
