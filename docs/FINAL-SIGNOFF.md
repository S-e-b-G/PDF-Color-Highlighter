# Recette finale M4c.5

Validation automatisée et installation Lenovo confirmées le 23 novembre 2026 (886 JVM / 31 suites natives / 94 scénarios). Les contrôles terrain ci-dessous sont distincts.

Aucune modification Kotlin, XML, Gradle, MuPDF ou automatisation. Utiliser des copies jetables, jamais les originaux. Ne pas relancer l’ensemble des tests Lenovo si le rapport FULL et le reçu correspondent déjà à l’APK utilisé.

| Contrôle | Action et résultat attendu |
| --- | --- |
| Traçabilité | Rapprocher le rapport FULL (886/31/94), le résultat d’installation Lenovo (`Success`) et l’APK de travail, jamais le paquet .autotest. Noter SHA-256, modèle, API et fournisseur. Si l’APK n’est plus identifiable, vérifier la provenance avant toute nouvelle validation; ne pas assimiler un nouveau build au binaire validé. |
| Teclast M40 API 29 | Installer l’APK de travail à signature compatible; ne jamais désinstaller pour forcer la mise à jour. Ouvrir TestFile.pdf puis vérifier rendu, zoom, rotation, sélection multicolonne, notes/surlignages, annuler/rétablir, Save As et réouverture. Vérifier aussi fermeture et reprise des modifications locales. |
| PDF réel importé | Sur copie jetable issue de l’outil tiers, modifier Contents et couleur, déplacer les poignées seulement lorsqu’elles sont proposées, supprimer puis annuler/rétablir. Enregistrer et rouvrir dans l’outil tiers. Vérifier identité, opacité et texte selon les actions; confirmer que les objets protégés ou ambigus restent intacts. |
| Fournisseurs Android | Tester Documents local et carte SD si disponible, puis le fournisseur réellement utilisé. Vérifier sauvegarde directe et Enregistrer sous par réouverture. Sur une copie jetable, provoquer un conflit externe: l’original modifié ailleurs ne doit pas être écrasé. Tester le dossier de secours autorisé puis révoquer l’accès: pas de succès trompeur, récupération locale conservée. |
| Extractions | Pour le même PDF final, comparer textes, couleurs et pages dans Obsidian et Docear. Les essais antérieurs ne valent pas validation de cette sortie finale. |
| Robustesse | Tester lecture seule, fichier signé/chiffré et entrée invalide ou inaccessible: refuser proprement l’action non permise sans mutation. Sur un PDF volumineux représentatif, noter les temps d’ouverture et de changement de page, zoom, rotation et éventuels ANR. |
| Accessibilité et diffusion | Vérifier TalkBack, libellés et ordre de focus, lisibilité et taille d’affichage sur tablette. Avant diffusion, contrôler licence/avis et sources correspondantes, signature, et absence de PDF privés, secrets et rapports sensibles dans les fichiers distribués. |

Pour chaque ligne, noter PASS / FAIL / NON TESTÉ, appareil, scénario et observation. Un échec reproductible motive un correctif ciblé; ne pas élargir le périmètre pour un cas non testé. Ne joindre ni PDF privé ni données de récupération aux rapports.

Rapport FULL : `.validation/autotest-reports/20261123-135356-27e9b8bae2a74c579ce0a4e55b6b37a5.zip`. APK de travail : `app\build\outputs\apk\debug\app-debug.apk` (un build ultérieur peut l’écraser). Ne pas utiliser l’APK isolé `.autotest`.

- Enregistrer sous : sur des copies jetables du même fournisseur, annuler le remplacement de B puis le confirmer, rouvrir B et vérifier que A sur disque reste inchangé. Le test natif ne couvre pas le sélecteur Android réel.
- PDF-XChange : vérifier texte enrichi et lien de réponse après recoloriage, puis annulation et confirmation de la suppression sur copies jetables.
- Docear et Obsidian : les essais antérieurs restent valides pour les fichiers testés ; la sortie finale M4c.5 n’est pas vérifiée par ces essais.
