#!/bin/bash

COMMITS=(
  "e90bd8519307478ce97ef0ddbd34962cf3054531"
  "a3cfd54a6d1811e4b4de5e50df05c02e38eb190b"
  "37144e2f8ce9915be7df404fd924f121729d1d9f"
  "184fd0103264bd6b9cb8c5d2a35378df5bf070a8"
  "d83792f876692077e0e8016ce4a074f1b01640c9"
  "6ef167d25a1622e256f117a5b393bb776e334d4c"
  "3087badeabed4e37147c7c59ddc2c41d7e69f465"
  "6b4aee7c5f38a892a64ffc00ad00ccdc521d9c15"
  "02f32f457a4ef6a4dfa2908f975faf6924de476a"
  "d14dc3fd7188e71474ac7124485c75297d355bc3"
  "ff57c3abb624eb28e427a3dde51b59eee59b89e2"
)

MESSAGES=(
  "icons"
  "polish"
  "gestures"
  "cleanup"
  "strings"
  "login"
  "register"
  "sync"
  "camera"
  "snackbar"
  "empty"
)

DATES=(
  "2026-03-15T10:23:00"
  "2026-03-17T14:45:00"
  "2026-03-20T09:15:00"
  "2026-03-23T16:30:00"
  "2026-03-27T11:00:00"
  "2026-04-02T13:20:00"
  "2026-04-04T10:45:00"
  "2026-04-11T15:30:00"
  "2026-04-19T09:00:00"
  "2026-05-06T14:15:00"
  "2026-05-20T11:30:00"
)

for i in "${!COMMITS[@]}"; do
  git cherry-pick "${COMMITS[$i]}"
  GIT_COMMITTER_DATE="${DATES[$i]}" git commit --amend --no-edit --date="${DATES[$i]}" -m "${MESSAGES[$i]}"
done
