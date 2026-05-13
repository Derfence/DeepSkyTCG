package fr.aumombelli.dstcg.feature.minigames

internal val QuizQuestionTemplates = listOf(
    QuizQuestionTemplate(
        id = "object-type",
        kind = QuizQuestionKind.ObjectType,
        depth = QuizQuestionDepth.Direct,
        prompt = { "Quel est le type d'objet indiqué pour ${it.cardName} ?" },
        answer = { it.objectType },
        explanation = { facts, answer -> "${facts.cardName} est indiqué comme : $answer." },
        controlledAnswers = { ObjectTypeAnswers },
    ),
    QuizQuestionTemplate(
        id = "object-family",
        kind = QuizQuestionKind.ObjectFamily,
        depth = QuizQuestionDepth.Direct,
        prompt = { "À quelle grande famille astronomique cette carte appartient-elle ?" },
        answer = { it.familyLabel },
        explanation = { facts, answer -> "${facts.cardName} appartient à la famille : $answer." },
        controlledAnswers = { ObjectFamilyAnswers },
    ),
    QuizQuestionTemplate(
        id = "main-season",
        kind = QuizQuestionKind.MainSeason,
        depth = QuizQuestionDepth.Direct,
        prompt = { "Quelle est sa saison principale d'observation ?" },
        answer = { it.mainSeason },
        explanation = { facts, answer -> "La saison principale indiquée pour ${facts.cardName} est : $answer." },
        controlledAnswers = { SeasonAnswers },
    ),
    QuizQuestionTemplate(
        id = "primary-catalog",
        kind = QuizQuestionKind.PrimaryCatalog,
        depth = QuizQuestionDepth.Direct,
        prompt = { "Quel est son catalogue principal ?" },
        answer = { it.primaryCatalog },
        explanation = { facts, answer -> "Le catalogue principal de ${facts.cardName} est : $answer." },
        controlledAnswers = { CatalogAnswers },
    ),
    QuizQuestionTemplate(
        id = "catalog-number",
        kind = QuizQuestionKind.CatalogNumber,
        depth = QuizQuestionDepth.Direct,
        prompt = { "Quelle est sa désignation dans ce catalogue ?" },
        answer = { it.catalogNumber },
        explanation = { facts, answer ->
            "${facts.cardName} est référencé comme $answer dans son catalogue principal."
        },
        controlledAnswers = { CatalogNumberAnswers },
    ),
    QuizQuestionTemplate(
        id = "constellation",
        kind = QuizQuestionKind.Constellation,
        depth = QuizQuestionDepth.Direct,
        prompt = { "Dans quelle constellation cette carte est-elle placée ?" },
        answer = { it.constellation },
        explanation = { facts, answer -> "${facts.cardName} est placé dans la constellation : $answer." },
        controlledAnswers = { ConstellationAnswers },
        isEligible = { it.constellation != null && it.profile != QuizObjectProfile.Constellation },
    ),
    QuizQuestionTemplate(
        id = "celestial-hemisphere",
        kind = QuizQuestionKind.CelestialHemisphere,
        depth = QuizQuestionDepth.SimpleDerived,
        prompt = { "Dans quel hémisphère céleste cette carte se situe-t-elle ?" },
        answer = { it.celestialHemisphere },
        explanation = { _, answer -> "Sa déclinaison la place dans : $answer." },
        controlledAnswers = { HemisphereAnswers },
    ),
    QuizQuestionTemplate(
        id = "distance-scale",
        kind = QuizQuestionKind.DistanceScale,
        depth = QuizQuestionDepth.Measurement,
        prompt = { "Quel ordre de grandeur décrit sa distance ?" },
        answer = { it.distanceScale?.answer },
        explanation = { facts, answer -> "La distance indiquée pour ${facts.cardName} correspond plutôt à : $answer." },
        controlledAnswers = { DistanceScaleAnswers },
        isEligible = { it.distanceScale != null },
        catalogFilter = { target, other -> target.distanceScale?.unit == other.distanceScale?.unit },
    ),
    QuizQuestionTemplate(
        id = "visual-moon-scale",
        kind = QuizQuestionKind.VisualMoonScale,
        depth = QuizQuestionDepth.Measurement,
        prompt = { "Comment sa taille apparente se compare-t-elle à la pleine Lune ?" },
        answer = { it.visualMoonScale },
        explanation = { facts, answer -> "La taille apparente indiquée pour ${facts.cardName} est : $answer." },
        controlledAnswers = { VisualMoonScaleAnswers },
        isEligible = { it.visualMoonScale != null },
    ),
    QuizQuestionTemplate(
        id = "real-size-scale",
        kind = QuizQuestionKind.RealSizeScale,
        depth = QuizQuestionDepth.Measurement,
        prompt = { "Quel ordre de grandeur décrit sa taille réelle indiquée ?" },
        answer = { it.realSizeScale?.answer },
        explanation = { facts, answer -> "La taille réelle indiquée pour ${facts.cardName} correspond plutôt à : $answer." },
        controlledAnswers = {
            when (it.realSizeScale?.unit) {
                QuizScaleUnit.Kilometers -> SolarSystemRealSizeAnswers
                QuizScaleUnit.LightYears -> AstronomicalRealSizeAnswers
                null -> SolarSystemRealSizeAnswers + AstronomicalRealSizeAnswers
            }
        },
        isEligible = { it.realSizeScale != null },
        catalogFilter = { target, other -> target.realSizeScale?.unit == other.realSizeScale?.unit },
    ),
    QuizQuestionTemplate(
        id = "absolute-magnitude",
        kind = QuizQuestionKind.AbsoluteMagnitudeClass,
        depth = QuizQuestionDepth.Measurement,
        prompt = { "Que suggère sa magnitude absolue ?" },
        answer = { it.magnitudeClass },
        explanation = { facts, answer -> "La magnitude absolue de ${facts.cardName} la classe comme : $answer." },
        controlledAnswers = { MagnitudeClassAnswers },
        isEligible = { it.magnitudeClass != null },
        catalogFilter = { target, other -> target.profile == other.profile },
    ),
    QuizQuestionTemplate(
        id = "profile-category",
        kind = QuizQuestionKind.ProfileCategory,
        depth = QuizQuestionDepth.ProfileSpecific,
        prompt = { "Que décrit principalement cette carte ?" },
        answer = { it.profileCategory },
        explanation = { facts, answer -> "${facts.cardName} décrit principalement $answer." },
        controlledAnswers = { ProfileCategoryAnswers },
    ),
    QuizQuestionTemplate(
        id = "solar-system-distance-context",
        kind = QuizQuestionKind.SolarSystemDistanceContext,
        depth = QuizQuestionDepth.ProfileSpecific,
        prompt = { "Quel type de distance est indiqué sur cette carte du Système solaire ?" },
        answer = { it.solarSystemDistanceContext },
        explanation = { facts, answer -> "La distance indiquée pour ${facts.cardName} correspond à $answer." },
        controlledAnswers = { SolarSystemDistanceContextAnswers },
        isEligible = { it.profile == QuizObjectProfile.SolarSystem && it.solarSystemDistanceContext != null },
        catalogFilter = { _, other -> other.profile == QuizObjectProfile.SolarSystem },
    ),
)

private val ObjectTypeAnswers = listOf(
    "Nébuleuse",
    "Galaxie",
    "Étoile",
    "Constellation",
    "Amas ouvert",
    "Amas globulaire",
    "Planète tellurique",
    "Géante gazeuse",
    "Satellite naturel",
    "Comète",
    "Essaim d'étoiles filantes",
    "Structure atmosphérique",
)

private val ObjectFamilyAnswers = listOf(
    "Système solaire",
    "Ciel profond",
    "Étoile",
    "Constellation",
    "Événement céleste",
    "Objet astronomique",
)

private val SeasonAnswers = listOf(
    "Hiver",
    "Printemps",
    "Été",
    "Automne",
    "Toute l'année",
    "Variable",
)

private val CatalogAnswers = listOf(
    "Messier",
    "NGC",
    "Bayer",
    "IAU",
    "Caldwell",
    "Planète",
    "Satellite naturel",
    "Système solaire",
    "IMO",
)

private val CatalogNumberAnswers = listOf(
    "M42",
    "M31",
    "NGC 1976",
    "Beta Cygni",
    "Sol III",
    "Jupiter IV",
    "C/2020 F3",
    "PER",
)

private val ConstellationAnswers = listOf(
    "Orion",
    "Cygne",
    "Lyre",
    "Aigle",
    "Andromède",
    "Taureau",
    "Sagittaire",
    "Scorpion",
    "Grande Ourse",
    "Persée",
)

private val HemisphereAnswers = listOf(
    "Hémisphère céleste nord",
    "Hémisphère céleste sud",
    "Équateur céleste",
    "Zone polaire céleste",
)

private val DistanceScaleAnswers = listOf(
    "moins de 10 années-lumière",
    "quelques dizaines d'années-lumière",
    "quelques centaines d'années-lumière",
    "quelques milliers d'années-lumière",
    "des dizaines de milliers d'années-lumière",
    "des millions d'années-lumière",
)

private val VisualMoonScaleAnswers = listOf(
    "bien plus petite que la pleine Lune",
    "plus petite que la pleine Lune",
    "comparable à la pleine Lune",
    "plus grande que la pleine Lune",
    "beaucoup plus étendue que la pleine Lune",
)

private val SolarSystemRealSizeAnswers = listOf(
    "moins de 100 km",
    "quelques centaines de kilomètres",
    "quelques milliers de kilomètres",
    "des dizaines de milliers de kilomètres",
    "plus de 50 000 km",
)

private val AstronomicalRealSizeAnswers = listOf(
    "moins d'un centième d'année-lumière",
    "moins d'une année-lumière",
    "quelques années-lumière",
    "des dizaines d'années-lumière",
    "des milliers d'années-lumière ou plus",
)

private val MagnitudeClassAnswers = listOf(
    "extrêmement lumineuse à l'échelle galactique",
    "très lumineuse intrinsèquement",
    "lumineuse intrinsèquement",
    "de luminosité intrinsèque modérée",
    "peu lumineuse intrinsèquement",
)

private val ProfileCategoryAnswers = listOf(
    "un objet ou phénomène du Système solaire",
    "un objet du ciel profond",
    "une étoile ou un système stellaire",
    "une constellation",
    "un événement céleste observable",
    "un objet astronomique",
)

private val SolarSystemDistanceContextAnswers = listOf(
    "une distance au Soleil en unités astronomiques",
    "une distance à un astre parent en kilomètres",
    "une distance locale exprimée en kilomètres",
    "une distance interstellaire en années-lumière",
)
