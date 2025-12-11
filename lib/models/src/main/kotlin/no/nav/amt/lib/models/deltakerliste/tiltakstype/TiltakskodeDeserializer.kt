package no.nav.amt.lib.models.deltakerliste.tiltakstype

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class TiltakskodeDeserializer : JsonDeserializer<Tiltakskode>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Tiltakskode {
        val text = p.text?.trim() ?: return ctxt.handleUnexpectedToken(Tiltakskode::class.java, p) as Tiltakskode
        return Tiltakskode.valueOf(text)
    }
}

