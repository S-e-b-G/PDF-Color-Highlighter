# Recette finale M4b-R2

Aucune modification Kotlin, XML, Gradle, MuPDF ou automatisation. Utiliser des copies jetables, jamais les originaux. Ne pas relancer l’ensemble des tests Lenovo si le rapport FULL et le reçu correspondent déjà à l’APK utilisé.

| Contrôle | Action et résultat attendu |
| --- | --- |
| Traçabilité | Rapprocher le rapport FULL (886/31/94), M4b-installation.json et l’APK de travail, jamais le paquet .autotest. Noter SHA-256, modèle, API et fournisseur. Si l’APK n’est plus identifiable, utiliser le validateur R2 local effectif; ne pas assimiler un nouveau build au binaire validé. |
| Teclast M40 API 29 | Installer l’APK de travail à signature compatible; ne jamais désinstaller pour forcer la mise à jour. Ouvrir TestFile.pdf puis vérifier rendu, zoom, rotation, sélection multicolonne, notes/surlignages, annuler/rétablir, Save As et réouverture. Vérifier aussi fermeture et reprise des modifications locales. |
| PDF réel importé | Sur copie jetable issue de l’outil tiers, modifier Contents et couleur, déplacer les poignées seulement lorsqu’elles sont proposées, supprimer puis annuler/rétablir. Enregistrer et rouvrir dans l’outil tiers. Vérifier identité, opacité et texte selon les actions; confirmer que les objets protégés ou ambigus restent intacts. |
| Fournisseurs Android | Tester Documents local et carte SD si disponible, puis le fournisseur réellement utilisé. Vérifier sauvegarde directe et Enregistrer sous par réouverture. Sur une copie jetable, provoquer un conflit externe: l’original modifié ailleurs ne doit pas être écrasé. Tester le dossier de secours autorisé puis révoquer l’accès: pas de succès trompeur, récupération locale conservée. |
| Extractions | Pour le même PDF final, comparer textes, couleurs et pages dans Obsidian et Docear. Obsidian a déjà été signalé OK; Docear sur cette sortie reste distinct. |
| Robustesse | Tester lecture seule, fichier signé/chiffré et entrée invalide ou inaccessible: refuser proprement l’action non permise sans mutation. Sur un PDF volumineux représentatif, noter les temps d’ouverture et de changement de page, zoom, rotation et éventuels ANR. |
| Accessibilité et diffusion | Vérifier TalkBack, libellés et ordre de focus, lisibilité et taille d’affichage sur tablette. Avant diffusion, contrôler licence/avis et sources correspondantes, signature, et absence de PDF privés, secrets et rapports sensibles dans les fichiers distribués. |

Pour chaque ligne, noter PASS / FAIL / NON TESTÉ, appareil, scénario et observation. Un échec reproductible motive un correctif ciblé; ne pas élargir le périmètre pour un cas non testé. Ne joindre ni PDF privé ni données de récupération aux rapports.
